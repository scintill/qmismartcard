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

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A QMI client. It is given stream to send the QMI on, and starts threads to read them
 * when start() is called.
 */

public class Client {
    private InputStream mInput;
    private OutputStream mOutput;
    private PrintStream mDebug;

    private BlockingQueue<Message> mOutputQueue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<Integer, MessageCallback> mCallbacks = new ConcurrentHashMap<>();
    private final List<MessageCallback> mIndicationHandlers = new ArrayList<>();

    private boolean mStopInputThread = false;
    private static final Message THE_STOP_MESSAGE = new Message();

    /**
     * @param in QMI input stream
     * @param out QMI output stream
     * @param debug an optional stream to output debug messages
     */
    public Client(InputStream in, OutputStream out, @Nullable PrintStream debug) throws IOException {
        mInput = in;
        mOutput = out;
        mDebug = debug;
    }

    /**
     * Start the processing on input/output QMI messages.
     */
    public void start() {
        new Thread(() -> {
            try {
                while (!mStopInputThread || mCallbacks.size() != 0) {
                    Message msg = Message.readFromInput(mInput);
                    if ((msg.getFlags() & Message.FLAG_INDICATION) == 0) {
                        // responses
                        MessageCallback callback = mCallbacks.remove(getCallbackKey(msg));
                        if (callback != null) {
                            callback.onReceive(msg);
                        }
                        debug("<< " + msg);
                    } else {
                        // indications
                        for (MessageCallback cb : mIndicationHandlers) {
                            cb.onReceive(msg);
                        }
                        // don't log indications, for now
                    }
                }

                debug("input thread stopping");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "MessagePumpInput").start();

        new Thread(() -> {
            try {
                for (;;) { // forever
                    // send output messages
                    Message msg = mOutputQueue.take();
                    if (msg == THE_STOP_MESSAGE) break;

                    msg.writeToOutput(mOutput);
                    debug(">> " + msg);
                }

                debug("output thread stopping");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "MessagePumpOutput").start();
    }

    /**
     * Stop the processing of QMI input/output messages.
     */
    public void stop() {
        mStopInputThread = true;
        deallocateClients();
        mOutputQueue.add(THE_STOP_MESSAGE);
    }

    /**
     * Initialize this message for sending -- give it a client ID and transaction ID.
     * An exception may be thrown if a client ID needs to be allocated, and something goes wrong in that process.
     * @param msg
     * @throws QmiException
     */
    private void prepareMessageForSending(Message msg) throws QmiException {
        if (msg.getServiceCode() != ServiceCode.Control) {
            msg.setClientId(this.getClientId(msg.getServiceCode()));
        }

        msg.setTxId(getTxId());
    }

    /**
     * Asynchronously send a message.
     * @param msg
     * @param callback
     * @throws QmiException
     */
    public void sendAsync(Message msg, MessageCallback callback) throws QmiException {
        prepareMessageForSending(msg);

        if (callback != null) {
            mCallbacks.put(getCallbackKey(msg), callback);
        }
        mOutputQueue.add(msg);
    }

    /**
     * Asynchronously send a message, without giving a callback for any response that may come.
     * @param msg
     * @throws QmiException
     */
    public void sendAsync(Message msg) throws QmiException {
        sendAsync(msg, null);
    }

    /**
     * Synchronously send a message. Wait for the response, or until the timeout (in ms).
     * @param msg
     * @param timeout timeout in ms
     * @return the response message
     * @throws QmiTimeoutException in case of timeout
     */
    public Message send(Message msg, int timeout) throws QmiException {
        final AtomicReference<Message> responseMsgHolder = new AtomicReference<>();
        sendAsync(msg, responseMsg -> {
            synchronized (responseMsgHolder) {
                responseMsgHolder.set(responseMsg);
                responseMsgHolder.notify();
            }
        });
        synchronized (responseMsgHolder) {
            if (responseMsgHolder.get() == null) {
                try {
                    responseMsgHolder.wait(timeout);
                } catch (InterruptedException e) {
                    throw new QmiException("interrupted");
                }
            }
        }

        Message responseMsg = responseMsgHolder.get();

        if (responseMsg == null) {
            // cancel callback, so input thread doesn't hang when we're ready to stop
            mCallbacks.remove(getCallbackKey(msg));
            throw new QmiTimeoutException();
        }

        throwQmiExceptionForMessageResult(responseMsg);
        return responseMsg;
    }

    /**
     * Synchronously send a message. Wait for the response.
     * @param msg
     * @return the response message
     */
    public Message send(Message msg) throws QmiException {
        return send(msg, 0);
    }

    private void debug(String msg) {
        if (mDebug != null) mDebug.println(msg);
    }

    private Map<ServiceCode, Short> mClientMap = new HashMap<>();
    /**
     * Get a client ID for the given service, asking the control service for a new one if necessary.
     * @param service
     * @return the client ID
     * @throws QmiException
     */
    private short getClientId(ServiceCode service) throws QmiException {
        // TODO racy
        if (mClientMap.get(service) == null) {
            Message allocMsg = new Message(ServiceCode.Control, 0x22);
            allocMsg.addTlvByte(0x01, service.value);
            Message allocResponse = send(allocMsg);
            Tlv serviceTlv = allocResponse.getTlv(1);
            if (serviceTlv == null) {
                throw new QmiException("unable to find service allocation tlv");
            }
            byte[] serviceTlvBytes = serviceTlv.getValue();
            if (serviceTlvBytes.length != 2) {
                throw new QmiException("unexpected servicetlv length");
            }
            if (((int) serviceTlvBytes[0] & 0xff) != service.value) {
                throw new QmiException("got unexpected service");
            }
            mClientMap.put(service, (short) (serviceTlvBytes[1] & 0xff));

            if (service == ServiceCode.Uim) {
                registerForUimIndications((byte) 7);
            }
        }

        return mClientMap.get(service);
    }

    /**
     * Unregister allocated client IDs with the QMI endpoint.
     * @return true if any QMI errors occurred while de-allocating
     */
    private boolean deallocateClients() {
        boolean qmiError = false;

        for (Map.Entry<ServiceCode, Short> clientPair : mClientMap.entrySet()) {
            Message deallocMsg = new Message(ServiceCode.Control, 0x23);
            deallocMsg.addTlvBytes(0x01, new byte[] { (byte) clientPair.getKey().value, clientPair.getValue().byteValue() });
            try {
                send(deallocMsg, 2500);
            } catch (QmiException e) {
                debug("error deallocating client "+clientPair.getKey()+": "+e);
                qmiError = true;
                // continue
            }
        }
        mClientMap.clear();

        return qmiError;
    }

    private static short txId = 1;
    /**
     * Get a transaction ID for a message.
     * @return the ID
     */
    private synchronized short getTxId() {
        return txId++;
    }

    /**
     * Raise a QMI exception, if the given message contains QMI error information.
     * @param msg
     * @throws QmiException
     */
    private void throwQmiExceptionForMessageResult(Message msg) throws QmiException {
        Tlv tlv02 = msg.getTlv(0x02);

        if (tlv02 == null) {
            throw new QmiException("no result TLV");
        }
        byte[] bytes = tlv02.getValue();
        if (bytes.length != 4) {
            throw new QmiException("invalid TLV 0x02 length");
        }
        if (bytes[0] != 0 || bytes[1] != 0) {
            throw new QmiErrorCodeException(QmiErrorCode.fromValue(
                    ((int) bytes[2]) | bytes[3] << 8)
            );
        }
    }

    /**
     * Get an int to use as key for the callback associated with the message.
     * @param msg message
     * @return the string key
     */
    private int getCallbackKey(Message msg) {
        return msg.getClientId() << 16 | msg.getTxId();
    }

    private void registerForUimIndications(byte mask) throws QmiException {
        Message msg = new Message(ServiceCode.Uim, 46);
        msg.addTlvBytes(1, new byte[] { mask, 0, 0, 0 });
        send(msg);
    }

    /**
     * Register for indications (unsolicited messages). Currently all of them are sent to all handlers.
     */
    public void registerForIndications(MessageCallback callback) {
        mIndicationHandlers.add(callback);
    }
}
