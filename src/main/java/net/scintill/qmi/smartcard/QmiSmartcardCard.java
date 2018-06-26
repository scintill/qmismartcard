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
import net.scintill.qmi.QmiException;
import net.scintill.qmi.SapClient;

import javax.smartcardio.*;
import java.nio.ByteBuffer;

public class QmiSmartcardCard extends Card {
    private byte slot;
    private Client client;
    private ATR atr;
    private SapClient sapClient;
    private QmiSmartcardTerminals.QmiSmartcardTerminal terminal;

    public QmiSmartcardCard(byte slot, Client client, QmiSmartcardTerminals.QmiSmartcardTerminal terminal) throws QmiException {
        this.slot = slot;
        this.client = client;
        this.terminal = terminal;

        this.sapClient = new SapClient(client, slot);
        SapClient.ConnectionStatus currentStatus = sapClient.getConnectionStatus();
        // we sometimes get stuck in Connecting state
        if (currentStatus == SapClient.ConnectionStatus.Connecting) {
            sapClient.disconnect(10000);
            // TODO sometimes need to wait here?
        }
        if (currentStatus != SapClient.ConnectionStatus.ConnectedSuccessfully) {
            sapClient.connect(10000);
        }

        this.atr = sapClient.getAtr();
    }

    @Override
    public ATR getATR() {
        return atr;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public CardChannel getBasicChannel() {
        return new BasicChannel();
    }

    @Override
    public CardChannel openLogicalChannel() throws CardException {
        throw new CardException("not supported");
    }

    @Override
    public void beginExclusive() throws CardException {
        throw new CardException("not supported");

    }

    @Override
    public void endExclusive() throws CardException {
        throw new CardException("not supported");

    }

    @Override
    public byte[] transmitControlCommand(int i, byte[] bytes) throws CardException {
        throw new CardException("not supported");
    }

    @Override
    // see SIMTester's OsmoCard for notes about the ambiguous meaning of this reset parameter
    public void disconnect(boolean notReset) throws CardException {
        try {
            if (!notReset) sapClient.resetSim();
        } catch (QmiException e) {
            throw new CardException("QMI error while resetting", e);
        }

        try {
            sapClient.disconnect(10000);
        } catch (QmiException e) {
            throw new CardException("QMI error while disconnecting", e);
        }

        terminal.cardDisconnectNotify();
        // TODO throw illegalstateexception if someone tries to use disconnected things? that's part of the contract...
    }

    class BasicChannel extends CardChannel {

        @Override
        public Card getCard() {
            return QmiSmartcardCard.this;
        }

        @Override
        public int getChannelNumber() {
            return 0;
        }

        @Override
        public ResponseAPDU transmit(CommandAPDU commandAPDU) throws CardException {
            try {
                return sapClient.sendApdu(commandAPDU);
            } catch (QmiException e) {
                throw new CardException("QMI error sending APDU", e);
            }

        }

        @Override
        public int transmit(ByteBuffer byteBuffer, ByteBuffer byteBuffer1) throws CardException {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void close() throws CardException {
            throw new RuntimeException("not implemented");
        }
    }

}
