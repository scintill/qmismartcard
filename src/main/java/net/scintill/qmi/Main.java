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

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        String path = "/dev/cdc-wdm1";

        try {
            Client client = new LinuxFileClient(path, System.err);
            client.start();

            Message msg = new Message(Message.Service.Dms, 0x24);
            Message resp = client.sendAndWait(msg);
            System.out.println("MSISDN="+resp.getTlv(1).getValueString());

            client.stop();
        } catch (IOException | Message.QmiException e) {
            throw new RuntimeException(e);
        }
    }

}
