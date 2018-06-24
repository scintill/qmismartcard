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

import java.security.Provider;

public class QmiSmartcardProvider extends Provider {

    /* package */ static Client sQmiClient;

    public QmiSmartcardProvider(Client qmiClient) {
        super("QmiSmartcardProvider", 1.0, "QmiSmartcardProvider");
        sQmiClient = qmiClient;
        put("TerminalFactory.QmiTerminalFactory", TerminalFactorySpi.class.getName());
    }
}
