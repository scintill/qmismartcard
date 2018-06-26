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

import javax.smartcardio.ATR;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A client for the QMI SIM Access Profile service (typically over bluetooth).
 */
public class SapClient implements MessageCallback {

    private final Client qmiClient;
    private final byte slot;
    private final AtomicReference<ConnectionStatus> connectionStatusHolder = new AtomicReference<>(ConnectionStatus.NotEnabled);

    // message codes
    private static final int SAP_CONNECT = 60;
    private static final int SAP_REQUEST = 61;

    public SapClient(Client qmiClient, byte slot) {
        this.qmiClient = qmiClient;
        this.slot = slot;
        qmiClient.registerForIndications(this);
    }

    public enum ConnectionStatus {
        NotEnabled,
        Connecting,
        ConnectedSuccessfully,
        ConnectionError,
        Disconnecting,
        DisconnectedSuccessfully;

        public static ConnectionStatus fromInt(int value) { return ConnectionStatus.values()[value]; }
    }

    /**
     * Connect to the SIM via SAP. Block until connected, or timeout occurs.
     * @param timeout in ms, or 0 for infinite
     * @throws QmiException
     * @return false if timeout or error
     */
    public boolean connect(int timeout) throws QmiException {
        return connect(true, timeout);
    }

    /**
     * Disonnect from the SIM via SAP. Block until disconnected, or timeout occurs.
     * @param timeout in ms, or 0 for infinite
     * @throws QmiException
     * @return false if timeout or error
     */
    public boolean disconnect(int timeout) throws QmiException {
        return connect(false, timeout);
    }

    /**
     * Connect/disconnect to the SIM via SAP. If connecting, block until connected, or timeout occurs.
     * @param isConnecting true to connect
     * @param timeout in ms, or 0 for infinite
     * @throws QmiException
     * @return false if timeout or error
     */
    private boolean connect(boolean isConnecting, int timeout) throws QmiException {
        sendSapMessage(SAP_CONNECT, isConnecting ? 1 : 0,
                //new Tlv((short) 0x12, new byte[] { 3 }) // connection condition = allow
                null
        );

        List<ConnectionStatus> finishedStatuses = Arrays.asList(isConnecting ?
            new ConnectionStatus[] { ConnectionStatus.ConnectedSuccessfully, ConnectionStatus.ConnectionError } :
            new ConnectionStatus[] { ConnectionStatus.DisconnectedSuccessfully, ConnectionStatus.NotEnabled });

        // TODO is this stupid, or just expectedly boilerplatey?
        if (!finishedStatuses.contains(connectionStatusHolder.get())) {
            synchronized (connectionStatusHolder) {
                try {
                    connectionStatusHolder.wait(timeout);
                } catch (InterruptedException e) { /* fall through */ }

                if (!finishedStatuses.contains(connectionStatusHolder.get())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Reset the SIM card.
     * @throws QmiException
     */
    public void resetSim() throws QmiException {
        sendSapMessage(SAP_REQUEST, 4);
        //sendSapMessage(SAP_REQUEST, 2); // power off
        //sendSapMessage(SAP_REQUEST, 3); // power on
    }

    public ConnectionStatus getConnectionStatus() throws QmiException {
        return ConnectionStatus.fromInt(sendSapMessage(SAP_CONNECT, 2).getTlv(0x10).getValue()[0]);
    }

    /**
     * Get the card's ATR value.
     * @return the ATR
     * @throws QmiException
     */
    public ATR getAtr() throws QmiException {
        return parseAtrTlv(sendSapMessage(SAP_REQUEST, 0).getTlv(0x10));
    }

    /**
     * Send an APDU to the card and return the response.
     * @param commandApdu
     * @return the response PDU
     * @throws QmiException
     */
    public ResponseAPDU sendApdu(CommandAPDU commandApdu) throws QmiException {
        // build TLV for APDU
        ByteBuffer bb = ByteBuffer.allocate(2 + commandApdu.getBytes().length);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // TODO does this correctly handle large 2-byte lengths ?
        bb.putShort((short) commandApdu.getBytes().length);
        bb.put(commandApdu.getBytes());

        Tlv apduTlv = new Tlv((short) 0x10, bb.array());

        // send APDU
        Message resp;
        try {
            resp = sendSapMessage(SAP_REQUEST, 1, apduTlv);
        } catch (QmiErrorCodeException e) {
            // TODO can we do better? AFAICT, the card is not denying this, the QMI service is
            if (e.getQmiErrorCode() == QmiErrorCode.AccessDenied) {
                return new ResponseAPDU(new byte[]{ (byte) 0x98, (byte) 0x04 }); // Access Condition not fulfilled
            } else {
                throw e;
            }
        }

        // parse response
        Tlv tlv = resp.getTlv(0x11);
        if (tlv == null) {
            throw new QmiException("APDU response TLV not returned");
        }
        byte[] b = tlv.getValue();
        if (b.length < 2) {
            throw new QmiException("invalid APDU response TLV");
        }
        int length = b[0] | b[1] << 8;
        if (length != b.length - 2) {
            throw new QmiException("invalid APDU response length");
        }

        return new ResponseAPDU(Arrays.copyOfRange(b, 2, b.length));
    }

    private Message sendSapMessage(int msgCode, int reqCode) throws QmiException {
        return sendSapMessage(msgCode, reqCode, null);
    }

    private Message sendSapMessage(int msgCode, int reqCode, Tlv addlTlv) throws QmiException {
        Message msg = new Message(ServiceCode.Uim, msgCode);
        msg.addTlvBytes(1, new byte[] { (byte) reqCode, slot });
        if (addlTlv != null) msg.addTlv(addlTlv);
        return qmiClient.send(msg);
    }

    @Override
    public void onReceive(Message msg) {
        if (msg.getServiceCode() == ServiceCode.Uim && msg.getMessageCode() == 62) {
            Tlv tlv = msg.getTlv(0x10);
            if (tlv != null) {
                byte[] b = tlv.getValue();
                if (b[1] == slot) {
                    synchronized (connectionStatusHolder) {
                        connectionStatusHolder.set(ConnectionStatus.fromInt(b[0]));
                        connectionStatusHolder.notify();
                    }
                }
            }
        }
    }

    /**
     * Parse a length-prefixed ATR from a TLV value.
     * @param tlv
     * @return
     */
    private static ATR parseAtrTlv(Tlv tlv) {
        byte[] b = tlv.getValue();
        if (b.length < 1) {
            throw new RuntimeException("invalid ATR TLV");
        }
        if ((b[0] & 0xff) != b.length - 1) {
            throw new RuntimeException("invalid ATR TLV length");
        }

        return new ATR(Arrays.copyOfRange(b, 1, b.length));
    }

}
