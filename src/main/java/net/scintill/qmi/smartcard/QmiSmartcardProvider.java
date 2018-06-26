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

import net.scintill.qmi.Client;
import net.scintill.qmi.LinuxFileClient;

import java.io.IOException;
import java.security.Provider;

public class QmiSmartcardProvider extends Provider {

    /* package */ static Client sQmiClient;

    public QmiSmartcardProvider() {
        // TODO include version stuff from phone
        super("QmiSmartcardProvider", 1.0, "QmiSmartcardProvider");
        try {
            // TODO make configurable
            sQmiClient = new LinuxFileClient("/dev/cdc-wdm0", null/*System.err*/);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sQmiClient.start();
        put("TerminalFactory.QmiTerminalFactory", TerminalFactorySpi.class.getName());
    }
}
