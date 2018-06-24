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

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A type-length-value container. The meanings of the types (two-byte integer)
 * and values (byte array) are not handled by this class.
 */
class Tlv {
    private short mType = -1;
    private byte[] mValue = null;

    public Tlv(short type, byte[] value) {
        this.mType = type;
        this.mValue = value;
    }

    /**
     * Get the byte-array value.
     * @return the byte-array value
     */
    public byte[] getValue() {
        return mValue;
    }

    /**
     * Get the value as an ASCII string.
     * @return value string
     */
    public String getValueString() {
        try {
            return new String(mValue, "ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the size required to serialize the given list of Tlvs.
     * @param tlvs
     * @return total size
     */
    public static short getSize(Iterable<Tlv> tlvs) {
        short size = 0;
        for (Tlv tlv : tlvs) {
            size += 1+2+ tlv.mValue.length;
        }
        return size;
    }

    /**
     * Write the given Tlvs to the buffer, which requires at least getSize() number of bytes.
     * @param tlvs
     * @param bb
     * @throws IOException
     */
    public static void writeToByteBuffer(Iterable<Tlv> tlvs, ByteBuffer bb) throws IOException {
        for (Tlv tlv : tlvs) {
            bb.put((byte)tlv.mType);
            bb.putShort((short)(tlv.mValue.length));
            bb.put(tlv.mValue);
        }
    }

    /**
     * Read Tlvs from the buffer, until expTotalLength bytes are read.
     * @param tlvs a map to add Tlvs to
     * @param expTotalLength the total length of the encoded Tlvs in the buffer
     * @param di the buffer to read from
     * @throws IOException
     */
    public static void readFromInput(Map<Integer, Tlv> tlvs, int expTotalLength, DataInput di) throws IOException {
        for (int totalLength = 0; totalLength < expTotalLength; ) {
            short type = (short) di.readUnsignedByte();
            int tlvLength = di.readUnsignedShort();
            byte[] value = new byte[tlvLength];
            di.readFully(value);
            Tlv tlv = new Tlv(type, value);

            tlvs.put((int)tlv.mType, tlv);
            totalLength += 1 + 2 + tlv.mValue.length;
        }
    }
}
