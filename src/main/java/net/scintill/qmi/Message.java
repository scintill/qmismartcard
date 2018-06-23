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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

class Message {
    private Service mService;
    private int mMessage;
    private int mClient;
    private int mFlags;
    private int mTxId;
    private final Map<Integer, Tlv> mTlvs = new HashMap<>();

    enum Service {
        Control(0),
        Wds(1),
        Dms(2),
        Nas(3),
        Qos(4),
        Wms(5),
        Pds(6),
        Auth(7),
        At(8),
        Voice(9),
        Cat2(10),
        Uim(11),
        Pbm(12),
        Qchat(13),
        Rmtfs(14),
        Test(15),
        Loc(16),
        Sar(17),
        Imss(18),
        Adc(19),
        Csd(20),
        Mfs(21),
        Time(22),
        Ts(23),
        Tmd(24),
        Sap(25),
        Wda(26),
        Tsync(27),
        Rfsa(28),
        Csvt(29),
        Qcmap(30),
        Imsp(31),
        Imsvt(32),
        Imsa(33),
        Coex(34),
        _res35(35),
        Pdc(36),
        _res37(37),
        Stx(38),
        Bit(39),
        Imsrtp(40),
        Rfrpe(41),
        Dsd(42),
        Ssctl(43),
        Cat(224),
        Rms(225),
        Oma(226),
        _Unknown(-1)
        ;

        Service(int value) { this.value = (byte) value; }
        public final short value;
        public static Service fromValue(int value) {
            // TODO slow
            for (Service s : Service.values()) {
                if (s.value == value) return s;
            }
            return _Unknown;
        }
    }

    public Message() {
        reset();
    }

    // TODO enumize message?
    public Message(Service service, int message) {
        reset();
        mService = service;
        mMessage = message;
    }

    public void reset() {
        mTlvs.clear();
        mTxId = getTransactionId(); // TODO ?
        mService = Service.Control;
        mMessage = 0;
    }

    public void addTlvByte(int type, int b) {
        addTlvBytes(type, new byte[] { (byte)b });
    }

    public void addTlvBytes(int type, byte[] bytes) {
        mTlvs.put(type, new Tlv((short)type, bytes));
    }

    public Tlv getTlv(int type) {
        return mTlvs.get(type);
    }

    public void writeToOutput(OutputStream os) throws IOException {
        /*
         * See also https://github.com/scintill/qmiserial2qmuxd/blob/d362b032ac8aaf8831afea840a07628acfe9b01f/qmiserial2qmuxd.c#L57
         * for structs, or GobiAPI, Linux kernel, libqmi, etc.
         */
        final boolean isControl = (mService == Service.Control);
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
        os.write(bb.array()); /* TODO efficiency? */
    }

    public static Message readFromInput(InputStream is) throws IOException {
        byte[] _buf = new byte[2048]; // libqmi reads into a 2k buffer
        int length = is.read(_buf);
        if (length < 0) {
            throw new IOException("EOF when reading");
        }

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(_buf, 0, length)); // TODO ugly?
        int b = dis.readUnsignedByte();
        if (b != 1) {
            throw new IOException("QMI serial framing error: frame byte was "+b);
        }

        int expLength = length-1;
        length = readShort(dis);
        if (length != expLength) {
            throw new IOException("invalid length. expected "+expLength+", got "+length);
        }

        b = dis.readUnsignedByte();
        if (b != 0x80) {
            throw new IOException("unexpected qmux flag value: "+b);
        }

        Message msg = new Message();
        msg.mService = Service.fromValue(dis.readUnsignedByte());
        msg.mClient = dis.readUnsignedByte();
        msg.mFlags = dis.readUnsignedByte();
        if (msg.mService == Service.Control) {
            msg.mTxId = dis.readUnsignedByte();
        } else {
            msg.mTxId = readShort(dis);
        }
        msg.mMessage = readShort(dis);

        int tlvLength = readShort(dis);
        Tlv.readFromInput(msg.mTlvs, tlvLength, dis);

        if (dis.available() != 0) {
            throw new IOException("did not parse entire message. "+dis.available()+" bytes remaining");
        }

        return msg;
    }

    public static int readShort(DataInput dis) throws IOException {
        // TODO little-endian reader?
        return dis.readUnsignedByte() | (dis.readUnsignedByte() << 8);
    }

    private static short txId = 1;
    private synchronized short getTransactionId() {
        return txId++;
    }

    /* package */ String getCallbackKey() {
        return mClient+","+mTxId;
    }

    @Override
    public String toString() {
        return mService+"("+mService.value+") msg="+mMessage+" client="+mClient+" txid="+mTxId;
    }

    public interface Callback {
        void onReceive(Message msg);
    }

    enum QmiErrorCode {
        None(0),
        MalformedMessage(1),
        NoMemory(2),
        Internal(3),
        Aborted(4),
        ClientIDsExhausted(5),
        UnabortableTransaction(6),
        InvalidClientID(7),
        NoThresholdsProvided(8),
        InvalidHandle(9),
        InvalidProfile(10),
        InvalidPINID(11),
        IncorrectPIN(12),
        NoNetworkFound(13),
        CallFailed(14),
        OutOfCall(15),
        NotProvisioned(16),
        MissingArgument(17),
        ArgumentTooLong(19),
        InvalidTransactionID(22),
        DeviceInUse(23),
        NetworkUnsupported(24),
        DeviceUnsupported(25),
        NoEffect(26),
        NoFreeProfile(27),
        InvalidPDPType(28),
        InvalidTechnologyPreference(29),
        InvalidProfileType(30),
        InvalidServiceType(31),
        InvalidRegisterAction(32),
        InvalidPSAttachAction(33),
        AuthenticationFailed(34),
        PINBlocked(35),
        PINAlwaysBlocked(36),
        UIMUninitialized(37),
        MaximumQoSRequestsInUse(38),
        IncorrectFlowFilter(39),
        NetworkQoSUnaware(40),
        InvalidQoSID(41),
        RequestedNumberUnsupported(42),
        InterfaceNotFound(43),
        FlowSuspended(44),
        InvalidDataFormat(45),
        GeneralError(46),
        UnknownError(47),
        InvalidArgument(48),
        InvalidIndex(49),
        NoEntry(50),
        DeviceStorageFull(51),
        DeviceNotReady(52),
        NetworkNotReady(53),
        WMSCauseCode(54),
        WMSMessageNotSent(55),
        WMSMessageDeliveryFailure(56),
        WMSInvalidMessageID(57),
        WMSEncoding(58),
        AuthenticationLock(59),
        InvalidTransition(60),
        NotMCASTInterface(61),
        MaximumMCASTRequestsInUse(62),
        InvalidMCASTHandle(63),
        InvalidIPFamilyPreference(64),
        SessionInactive(65),
        SessionInvalid(66),
        SessionOwnership(67),
        InsufficientResources(68),
        Disabled(69),
        InvalidOperation(70),
        InvalidQMICommand(71),
        WMSTPDUType(72),
        WMSSMSCAddress(73),
        InformationUnavailable(74),
        SegmentTooLong(75),
        SegmentOrder(76),
        BundlingNotSupported(77),
        OperationPartialFailure(78),
        PolicyMismatch(79),
        SIMFileNotFound(80),
        ExtendedInternal(81),
        AccessDenied(82),
        HardwareRestricted(83),
        AckNotSent(84),
        InjectTimeout(85),
        IncompatibleState(90),
        FDNRestrict(91),
        SUPSFailureCause(92),
        NoRadio(93),
        NotSupported(94),
        NoSubscription(95),
        CardCallControlFailed(96),
        NetworkAborted(97),
        MSGBlocked(98),
        InvalidSessionType(100),
        InvalidPBType(101),
        NoSIM(102),
        PBNotReady(103),
        PINRestriction(104),
        PIN2Restriction(105),
        PUKRestriction(106),
        PUK2Restriction(107),
        PBAccessRestricted(108),
        PBDeleteInProgress(109),
        PBTextTooLong(110),
        PBNumberTooLong(111),
        PBHiddenKeyRestriction(112),
        PBNotAvailable(113),
        CATEventRegistrationFailed(61441),
        CATInvalidTerminalResponse(61442),
        CATInvalidEnvelopeCommand(61443),
        CATEnvelopeCommandBusy(61444),
        CATEnvelopeCommandFailed(61445),
        _Unknown(-1),
        ;

        private int value;
        QmiErrorCode(int value) { this.value = value; }
        public static QmiErrorCode fromValue(int value) {
            // TODO slow
            for (QmiErrorCode s : QmiErrorCode.values()) {
                if (s.value == value) return s;
            }
            return _Unknown;
        }
    }

    public static class QmiException extends Exception {
        public QmiException(String s) {
            super(s);
        }
    }

    public class QmiErrorCodeException extends QmiException {
        public QmiErrorCodeException(QmiErrorCode ec) {
            super("ErrorCode "+ec);
        }
    }

    public static class QmiTimeoutException extends QmiException {
        public QmiTimeoutException() {
            super("Timeout");
        }
    }

    public void raiseQmiException(Tlv tlv02) throws QmiException {
        if (tlv02 == null) {
            throw new QmiException("no result TLV");
        }
        byte[] bytes = tlv02.getValue();
        if (bytes.length != 4) {
            throw new QmiException("invalid TLV 0x02 length");
        }
        if (bytes[0] != 0 || bytes[1] != 0) {
            throw new QmiErrorCodeException(QmiErrorCode.fromValue(
                    ((int) bytes[2]) | bytes[3] << 8)
            );
        }
    }

    public Service getService() {
        return mService;
    }

    public void setClientId(int clientId) {
        mClient = clientId;
    }
}
