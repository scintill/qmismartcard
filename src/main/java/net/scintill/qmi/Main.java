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

import de.srlabs.simlib.ChannelHandler;
import de.srlabs.simlib.CommonFileReader;
import net.scintill.qmi.smartcard.QmiSmartcardProvider;

import javax.smartcardio.CardException;
import java.io.IOException;
import java.security.Security;

public class Main {

    public static void main(String[] args) {
        String path = "/dev/cdc-wdm1";

        try {
            Client client = new LinuxFileClient(path, System.err);
            client.start();

            Security.insertProviderAt(new QmiSmartcardProvider(client), 1);
            ChannelHandler.getInstance(0, "QmiTerminalFactory");
            System.out.println("ICCID=" + CommonFileReader.readICCID());

            client.stop();
        } catch (IOException | CardException e) {
            throw new RuntimeException(e);
        }
    }

}
