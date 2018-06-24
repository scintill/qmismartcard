/*
 * This file is part of qmismartcard.
 *
 * qmismartcard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * qmismartcard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with qmismartcard.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.scintill.qmi;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * A QMI message. Messages are sent to a ServiceCode, have a message code, and optionally.
 * TLV parameters.
 */
public class Message {
    private ServiceCode mService;
    private int mMessage;
    private int mClient;
    private int mFlags;
    private int mTxId;
    private final Map<Integer, Tlv> mTlvs = new HashMap<>();

    public Message() {
        reset();
    }

    public Message(ServiceCode service, int message) {
        reset();
        mService = service;
        mMessage = message;
    }

    /**
     * Clear TLVs, and set txID, service, and message to placeholder values.
     */
    public void reset() {
        mTlvs.clear();
        mTxId = -1;
        mService = ServiceCode._Unknown;
        mMessage = -1;
    }

    /**
     * Add a 1-byte TLV.
     * @param type the TLV type code
     * @param b the byte value
     */
    public void addTlvByte(int type, int b) {
        addTlvBytes(type, new byte[] { (byte)b });
    }

    /**
     * Add a multi-byte TLV.
     * @param type the TLV type code
     * @param bytes the byte values
     */
    public void addTlvBytes(int type, byte[] bytes) {
        mTlvs.put(type, new Tlv((short)type, bytes));
    }

    /**
     * Add a TLV.
     * @param tlv
     */
    public void addTlv(Tlv tlv) {
        mTlvs.put(tlv.getType(), tlv);
    }

    /**
     * Get the the given TLV by type code.
     * @param type the type code
     * @return
     */
    public Tlv getTlv(int type) {
        return mTlvs.get(type);
    }

    /**
     * Write this message to the output stream.
     * @param os
     * @throws IOException
     */
    public void writeToOutput(OutputStream os) throws IOException {
        /*
         * See also https://github.com/scintill/qmiserial2qmuxd/blob/d362b032ac8aaf8831afea840a07628acfe9b01f/qmiserial2qmuxd.c#L57
         * for structs, or GobiAPI, Linux kernel, libqmi, etc.
         */
        final boolean isControl = (mService == ServiceCode.Control);
        final int tlvSize = Tlv.getSize(mTlvs.values());

        ByteBuffer bb = ByteBuffer.allocate(1+5+(isControl ? 4 : 5)+2+tlvSize);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.put((byte)1); // serial frame
        // qmux header
        bb.putShort((short)(bb.limit()-1)); // length
        bb.put((byte)0); // flags
        bb.put((byte)mService.value); // service
        bb.put(isControl ? 0 : (byte) mClient); // client
        // control/service header
        bb.put((byte)0); // flags
        if (isControl) {
            bb.put((byte)mTxId);
        } else {
            bb.putShort((short)mTxId);
        }
        bb.putShort((short)mMessage);
        // tlv
        bb.putShort((short)tlvSize);
        Tlv.writeToByteBuffer(mTlvs.values(), bb);

        // write out
        os.write(bb.array());
    }

    /**
     * Read the next message from the input stream (may block).
     * @param is
     * @return the message
     * @throws IOException
     */
    public static Message readFromInput(InputStream is) throws IOException {
        byte[] buf = new byte[2048]; // libqmi reads into a 2k buffer
        int length = is.read(buf);
        if (length < 0) {
            throw new IOException("EOF when reading");
        }

        LittleEndianDataInputStream di = new LittleEndianDataInputStream(new ByteArrayInputStream(buf, 0, length));

        int b = di.readUnsignedByte();
        if (b != 1) {
            throw new IOException("QMI serial framing error: frame byte was "+b);
        }

        int expLength = length-1;
        length = di.readUnsignedShort();
        if (length != expLength) {
            throw new IOException("invalid length. expected "+expLength+", got "+length);
        }

        b = di.readUnsignedByte();
        if (b != 0x80) {
            throw new IOException("unexpected qmux flag value: "+b);
        }

        Message msg = new Message();
        msg.mService = ServiceCode.fromValue(di.readUnsignedByte());
        msg.mClient = di.readUnsignedByte();
        msg.mFlags = di.readUnsignedByte();
        if (msg.mService == ServiceCode.Control) {
            msg.mTxId = di.readUnsignedByte();
        } else {
            msg.mTxId = di.readUnsignedShort();
        }
        msg.mMessage = di.readUnsignedShort();

        int tlvLength = di.readUnsignedShort();
        Tlv.readFromInput(msg.mTlvs, tlvLength, di);

        if (di.available() != 0) {
            throw new IOException("did not parse entire message. "+di.available()+" bytes remaining");
        }

        return msg;
    }

    @Override
    public String toString() {
        return mService+"("+mService.value+") msg="+mMessage+" client="+mClient+" txid="+mTxId+" flags="+mFlags;
    }

    /**
     * Set the transaction ID.
     * @param txId
     */
    public void setTxId(int txId) {
        mTxId = txId;
    }

    public int getTxId() {
        return mTxId;
    }

    /**
     * Get the service code that this message is from/to.
     * @return the code
     */
    public ServiceCode getServiceCode() {
        return mService;
    }

    /**
     * Get the client ID that this message is from/to.
     * @param clientId
     */
    public void setClientId(int clientId) {
        mClient = clientId;
    }

    public int getClientId() {
        return mClient;
    }

    public int getFlags() { return mFlags; }

    public int getMessageCode() { return mMessage; }

    static final int FLAG_INDICATION = 4;
}
