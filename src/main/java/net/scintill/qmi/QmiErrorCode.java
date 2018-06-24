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
