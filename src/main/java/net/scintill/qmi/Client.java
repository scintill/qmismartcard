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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class Client {
    private InputStream mInput;
    private OutputStream mOutput;
    private PrintStream mDebug;
    private BlockingQueue<Message> mOutputQueue = new LinkedBlockingQueue<>();
    private Map<String, Message.Callback> mCallbacks = new HashMap<>();

    private boolean mStopInputThread = false;
    private static final Message STOP_MESSAGE = new Message();

    public Client(InputStream in, OutputStream out, PrintStream debug) {
        mInput = in;
        mOutput = out;
        mDebug = debug;
    }

    public void start() {
        new Thread(() -> {
            try {
                while (!mStopInputThread || mCallbacks.size() != 0) {
                    Message msg = Message.readFromInput(mInput);
                    Message.Callback callback = mCallbacks.remove(msg.getCallbackKey());
                    if (callback != null) {
                        callback.onReceive(msg);
                    }
                    debug("<< " + msg);
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
                    if (msg == STOP_MESSAGE) break;

                    msg.writeToOutput(mOutput);
                    debug(">> " + msg);
                }

                debug("output thread stopping");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "MessagePumpOutput").start();
    }

    public void stop() {
        mStopInputThread = true;
        deallocateClients();
        mOutputQueue.add(STOP_MESSAGE);
    }

    public void send(Message msg, Message.Callback callback) throws Message.QmiException {
        if (msg.getService() != Message.Service.Control) {
            msg.setClientId(this.getClientId(msg.getService()));
        }

        if (callback != null) {
            mCallbacks.put(msg.getCallbackKey(), callback);
        }
        mOutputQueue.add(msg);
    }

    public void send(Message msg) throws Message.QmiException {
        send(msg, null);
    }

    public Message sendAndWait(Message msg, int timeout) throws Message.QmiException {
        final AtomicReference<Message> responseMsgHolder = new AtomicReference<>();
        send(msg, responseMsg -> {
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
                    throw new Message.QmiException("interrupted");
                }
            }
        }

        Message responseMsg = responseMsgHolder.get();

        if (responseMsg == null) {
            throw new Message.QmiTimeoutException();
        }

        responseMsg.raiseQmiException(responseMsg.getTlv(0x02));
        return responseMsg;
    }

    public Message sendAndWait(Message msg) throws Message.QmiException {
        return sendAndWait(msg, 0);
    }

    private void debug(String msg) {
        if (mDebug != null) mDebug.println(msg);
    }

    private Map<Message.Service, Short> mClientMap = new HashMap<>();
    private short getClientId(Message.Service service) throws Message.QmiException {
        if (mClientMap.get(service) == null) {
            Message allocMsg = new Message(Message.Service.Control, 0x22);
            allocMsg.addTlvByte(0x01, service.value);
            Message allocResponse = sendAndWait(allocMsg);
            Tlv serviceTlv = allocResponse.getTlv(1);
            if (serviceTlv == null) {
                throw new Message.QmiException("unable to find service allocation tlv");
            }
            byte[] serviceTlvBytes = serviceTlv.getValue();
            if (serviceTlvBytes.length != 2) {
                throw new Message.QmiException("unexpected servicetlv length");
            }
            if (((int) serviceTlvBytes[0] & 0xff) != service.value) {
                throw new Message.QmiException("got unexpected service");
            }
            mClientMap.put(service, (short) (serviceTlvBytes[1] & 0xff));
        }

        return mClientMap.get(service);
    }

    private void deallocateClients() {
        for (Map.Entry<Message.Service, Short> clientPair : mClientMap.entrySet()) {
            Message deallocMsg = new Message(Message.Service.Control, 0x23);
            deallocMsg.addTlvBytes(0x01, new byte[] { (byte) clientPair.getKey().value, clientPair.getValue().byteValue() });
            try {
                sendAndWait(deallocMsg, 2500);
            } catch (Message.QmiException e) {
                debug("error deallocating client "+clientPair.getKey()+": "+e);
                // continue
            }
        }
        mClientMap.clear();
    }
}
