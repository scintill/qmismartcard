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

import java.lang.reflect.InvocationTargetException;
import java.security.Security;

public class SIMTesterMain {

    public static void main(String[] args) {
        System.setProperty("javax.smartcardio.TerminalFactory.DefaultType", "QmiTerminalFactory");
        Security.insertProviderAt(new QmiSmartcardProvider(), 1);

        /*
        try {
            ChannelHandler.getInstance(0, "QmiTerminalFactory");
            System.out.println("ICCID=" + CommonFileReader.readICCID());
            System.out.println("MSISDN=" + CommonFileReader.decodeMSISDN(CommonFileReader.readRawMSISDN()));
            FileManagement.selectFileById(new byte[]{63, 0});
            System.exit(1);
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
        */

        try {
            Class.forName("de.srlabs.simtester.SIMTester").getMethod("main", args.getClass()).invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error running SIMTester", e.getCause());
        } catch (IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Error running SIMTester", e);
        }
    }

}
