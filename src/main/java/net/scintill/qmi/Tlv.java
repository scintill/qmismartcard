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

/**
 * A type-length-value container. The meanings of the types (two-byte integer)
 * and values (byte array) are not address in this class.
 */

package net.scintill.qmi;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

class Tlv {
    private short mType = -1;
    private byte[] mValue = null;

    public Tlv() {
    }

    public Tlv(short type, byte[] value) {
        this.mType = type;
        this.mValue = value;
    }

    public byte[] getValue() {
        return mValue;
    }

    public String getValueString() {
        try {
            return new String(mValue, "ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static short getSize(Iterable<Tlv> tlvs) {
        short size = 0;
        for (Tlv tlv : tlvs) {
            size += 1+2+ tlv.mValue.length;
        }
        return size;
    }

    public static void writeToByteBuffer(Iterable<Tlv> tlvs, ByteBuffer bb) throws IOException {
        for (Tlv tlv : tlvs) {
            bb.put((byte)tlv.mType);
            bb.putShort((short)(tlv.mValue.length));
            bb.put(tlv.mValue);
        }
    }

    public static void readFromInput(Map<Integer, Tlv> tlvs, int expTotalLength, DataInput dis) throws IOException {
        for (int totalLength = 0; totalLength < expTotalLength; ) {
            Tlv tlv = new Tlv();
            tlv.mType = (short) dis.readUnsignedByte();
            int tlvLength = Message.readShort(dis);
            tlv.mValue = new byte[tlvLength];
            dis.readFully(tlv.mValue);

            tlvs.put((int)tlv.mType, tlv);
            totalLength += 1 + 2 + tlv.mValue.length;
        }
    }
}
