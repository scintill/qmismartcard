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

package net.scintill.qmi.smartcard;

import net.scintill.qmi.*;

import javax.smartcardio.*;
import java.nio.*;
import java.util.Arrays;

public class QmiSmartcardCard extends Card {
    private int slot;
    private Client client;
    private ATR atr;

    public QmiSmartcardCard(int slot, Client client) throws QmiException {
        this.slot = slot;
        this.client = client;

        // Get ATR
        Message msg = new Message(ServiceCode.Uim, 0x41);
        msg.addTlvByte(1, slot);
        Message resp = client.send(msg);

        Tlv tlv = resp.getTlv(0x10);
        byte[] b = tlv.getValue();
        if (b.length < 1) {
            throw new RuntimeException("invalid ATR TLV");
        }
        if ((b[0] & 0xff) != b.length-1) {
            throw new RuntimeException("invalid ATR TLV length");
        }

        atr = new ATR(Arrays.copyOfRange(b, 1, b.length-2));
    }

    @Override
    public ATR getATR() {
        return atr;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public CardChannel getBasicChannel() {
        return new BasicChannel();
    }

    @Override
    public CardChannel openLogicalChannel() throws CardException {
        throw new CardException("not supported");
    }

    @Override
    public void beginExclusive() throws CardException {
        throw new CardException("not supported");

    }

    @Override
    public void endExclusive() throws CardException {
        throw new CardException("not supported");

    }

    @Override
    public byte[] transmitControlCommand(int i, byte[] bytes) throws CardException {
        throw new CardException("not supported");
    }

    @Override
    public void disconnect(boolean b) throws CardException {
        throw new CardException("not supported");
    }

    class BasicChannel extends CardChannel {

        @Override
        public Card getCard() {
            return QmiSmartcardCard.this;
        }

        @Override
        public int getChannelNumber() {
            return 0;
        }

        @Override
        public ResponseAPDU transmit(CommandAPDU commandAPDU) throws CardException {
            Message msg = new Message(ServiceCode.Uim, 0x3B);
            msg.addTlvByte(0x01, slot);

            // build TLV for PDU
            ByteBuffer bb = ByteBuffer.allocate(2 + commandAPDU.getBytes().length);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            bb.putShort((short) commandAPDU.getBytes().length);
            bb.put(commandAPDU.getBytes());
            msg.addTlvBytes(0x02, bb.array());

            Message resp;
            try {
                resp = client.send(msg);
            } catch (QmiException e) {
                throw new CardException("QMI error sending APDU", e);
            }

            // parse response
            Tlv tlv = resp.getTlv(0x10);
            if (tlv == null) {
                throw new CardException("APDU response TLV not returned");
            }
            byte[] b = tlv.getValue();
            if (b.length < 2) {
                throw new CardException("invalid APDU response TLV");
            }
            int length = b[0] | b[1] << 8;
            if (length != b.length - 2) {
                throw new CardException("invalid APDU response length");
            }

            return new ResponseAPDU(Arrays.copyOfRange(b, 2, b.length-3));
        }

        @Override
        public int transmit(ByteBuffer byteBuffer, ByteBuffer byteBuffer1) throws CardException {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void close() throws CardException {
            throw new RuntimeException("not implemented");
        }
    }

}
