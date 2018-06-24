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

enum ServiceCode {
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

    ServiceCode(int value) { this.value = (byte) value; }
    public final short value;
    public static ServiceCode fromValue(int value) {
        // TODO slow
        for (ServiceCode s : ServiceCode.values()) {
            if (s.value == value) return s;
        }
        return _Unknown;
    }
}
