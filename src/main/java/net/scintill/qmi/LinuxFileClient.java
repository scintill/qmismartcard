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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * A QMI client over a Linux file (probably cdc-wdm).
 */
public class LinuxFileClient extends Client {

    /**
     * Construct a QMI client over a Linux file (probably cdc-wdm).
     * @param path path to the file
     * @param debug debug stream
     * @throws IOException
     */
    public LinuxFileClient(String path, PrintStream debug) throws IOException {
        super(new FileInputStream(path), new FileOutputStream(path), debug);
    }

}
