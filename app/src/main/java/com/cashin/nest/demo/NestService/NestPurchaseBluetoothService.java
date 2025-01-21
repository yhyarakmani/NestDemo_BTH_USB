package com.cashin.nest.demo.NestService;


import static com.cashin.nest.demo.NestService.NestService.STATE_CONNECTED;
import static com.cashin.nest.demo.NestService.NestService.STATE_NONE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.cashin.nest.demo.enums.PurchaseActions;
import com.cashin.nest.demo.models.requests.PurchaseRequest;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import timber.log.Timber;

public class NestPurchaseBluetoothService {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String END_OF_MESSAGE = "\n$";
    public static final String TOAST = "toast";
    private static final String TAG = "BluetoothChatService";
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("af19f439-896b-48d1-b8cb-99c605a5543d");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("3b1c62ee-55b4-4f4b-808c-4e55cd2d583e");
    // Member fields
    private final BluetoothAdapter mAdapter;
    NestService.UseCaseCallback<Throwable, String, String> callback;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    // Constants that indicate the current connection state
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private String deviceName;
    private final Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Timber.i("MESSAGE_STATE_CHANGE: %s", msg.arg1);
                switch (msg.arg1) {
                    case STATE_CONNECTED:
                        callback.onStatusChangedResponse("Connected to" + " " + deviceName, STATE_CONNECTED);
                        break;
                    case STATE_CONNECTING:
                        callback.onStatusChangedResponse("Connecting", STATE_CONNECTING);
                        break;
                    case STATE_LISTEN:
                    case STATE_NONE:
                        callback.onStatusChangedResponse("Connection failed", STATE_NONE);
                        break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                Timber.e("Me:  %s", writeMessage);
                break;
            case MESSAGE_READ:
//                byte[] readBuf = (byte[]) msg.obj;
//                // construct a string from the valid bytes in the buffer
//                String readMessage = new String(readBuf, 0, msg.arg1);
                Timber.e("read message:  %s", msg.obj.toString());
                callback.onListenerInvoked(msg.obj.toString());
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                //mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                callback.onStatusChangedResponse("Connected to" + " " + deviceName, MESSAGE_DEVICE_NAME);
                break;
            case MESSAGE_TOAST:
                callback.onStatusChangedResponse(msg.getData().getString(TOAST), MESSAGE_TOAST);
                break;
        }
        return true;
    });

    public NestPurchaseBluetoothService(NestService.UseCaseCallback<Throwable, String, String> callback) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        this.callback = callback;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Timber.tag(TAG).d("setState() " + mState + " -> " + state);
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Timber.tag(TAG).d("start");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_LISTEN);
        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
       /* if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }*/
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param bluetoothDevice The data manager to get the device to connect
     * @param secure          Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice bluetoothDevice, boolean secure) {
        try {

            if (mConnectedThread != null) {
                setState(STATE_CONNECTED);
                return;
            }

            deviceName = bluetoothDevice.getName();
            if (mState == STATE_CONNECTING) {
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            }
            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(bluetoothDevice, secure);
            mConnectThread.start();
            setState(STATE_CONNECTING);

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Timber.tag(TAG).d("connected, Socket Type:%s", socketType);
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Timber.tag(TAG).d("stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Start the service over to restart listening mode
        NestPurchaseBluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Start the service over to restart listening mode
        NestPurchaseBluetoothService.this.start();
    }

    public void checkVersion() {
        launchNestPurchaseAction(PurchaseActions.CheckNestVersion.getType(), null);
    }

    public void getAvailablePaymentMethods() {
        launchNestPurchaseAction(PurchaseActions.GetAvailablePaymentMethods.getType(), null);
    }

    public boolean pay(String uuid, String phone, String name, double amountToPay, int nestPaymentType) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setCustomerPhone(phone);
        purchaseRequest.setCustomerName(name);
        purchaseRequest.setCustomerReferenceNumber(uuid);
        purchaseRequest.setAmount((long) Helper.round(amountToPay * 100, 2));
        purchaseRequest.setPaymentMethod(nestPaymentType);

        return launchNestPurchaseAction(PurchaseActions.Payment.getType(), purchaseRequest);
    }



    private boolean launchNestPurchaseAction(int actionType, PurchaseRequest purchaseRequest) {
        try {
            if (getState() != STATE_CONNECTED)
                return false;
            // If purchaseRequest is null, create a new one and set base fields
            if (purchaseRequest == null) {
                purchaseRequest = new PurchaseRequest();
            }
            purchaseRequest.setAction(actionType);
            purchaseRequest.setPackageName("com.cashin.nestDemo");

            // Serialize the PurchaseRequest object to JSON
            Gson gson = new Gson();
            String requestJson = gson.toJson(purchaseRequest) + END_OF_MESSAGE;
            write(requestJson.getBytes());
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private BluetoothServerSocket mmServerSocket = null;
        private final String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";
            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Socket Type: " + mSocketType + "listen() failed");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            try {
                Timber.tag(TAG).d("Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this);
                setName("AcceptThread" + mSocketType);
                BluetoothSocket socket;
                // Listen to the server socket if we're not connected
                while (mState != STATE_CONNECTED) {
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        socket = mmServerSocket.accept();
                    } catch (IOException e) {
                        Timber.tag(TAG).e(e, "Socket Type: " + mSocketType + "accept() failed");
                        break;
                    }
                    // If a connection was accepted
                    if (socket != null) {
                        synchronized (NestPurchaseBluetoothService.this) {
                            switch (mState) {
                                case STATE_LISTEN:
                                case STATE_CONNECTING:
                                    // Situation normal. Start the connected thread.
                                    connected(socket, socket.getRemoteDevice(),
                                            mSocketType);
                                    break;
                                case STATE_NONE:
                                case STATE_CONNECTED:
                                    // Either not ready or already connected. Terminate new socket.
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        Timber.tag(TAG).e(e, "Could not close unwanted socket");
                                    }
                                    break;
                            }
                        }
                    }
                }
                Timber.tag(TAG).i("END mAcceptThread, socket Type: %s", mSocketType);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        public void cancel() {
            Timber.tag(TAG).d("Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Socket Type" + mSocketType + "close() of server failed");
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket = null;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Socket Type: " + mSocketType + "create() failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            Timber.tag(TAG).i("BEGIN mConnectThread SocketType:%s", mSocketType);
            setName("ConnectThread" + mSocketType);
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Timber.tag(TAG).e(e2, "unable to close() " + mSocketType +
                            " socket during connection failure");
                }
                connectionFailed();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (NestPurchaseBluetoothService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "close() of connect " + mSocketType + " socket failed");
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Timber.tag(TAG).d("create ConnectedThread: %s", socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "temp sockets not created");
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Timber.tag(TAG).i("BEGIN mConnectedThread");
            byte[] buffer = new byte[1024]; // Adjusted buffer size for chunks
            int bytes;
            StringBuilder fullMessage = new StringBuilder(); // Accumulate data here

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Convert bytes to a string and accumulate in fullMessage
                    String chunk = new String(buffer, 0, bytes);
                    fullMessage.append(chunk);

                    // Check if the message is complete (e.g., contains a delimiter)
                    int delimiterIndex;
                    while ((delimiterIndex = fullMessage.indexOf(END_OF_MESSAGE)) != -1) {
                        // Extract complete message
                        String completeMessage = fullMessage.substring(0, delimiterIndex);

                        // Remove the processed message from the buffer
                        fullMessage.delete(0, delimiterIndex + END_OF_MESSAGE.length());

                        // Send the complete message to the UI Activity
                        mHandler.obtainMessage(MESSAGE_READ, completeMessage.length(), -1, completeMessage)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    Timber.tag(TAG).e(e, "disconnected");
                    connectionLost();
                    // Start the service over to restart listening mode
                    NestPurchaseBluetoothService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                // Define the maximum chunk size based on Bluetooth MTU limits
                int chunkSize = 512; // Adjust based on your Bluetooth device's MTU
                int bytesSent = 0;
                int bufferLength = buffer.length;

                // Loop through the buffer and send chunks
                while (bytesSent < bufferLength) {
                    int bytesToSend = Math.min(chunkSize, bufferLength - bytesSent);
                    mmOutStream.write(buffer, bytesSent, bytesToSend);
                    bytesSent += bytesToSend;

                    // Optional: delay between chunks, depending on the device's handling capacity
                    // Thread.sleep(10);  // Uncomment if you encounter issues with rapid sending
                }

                // Share the entire sent message back to the UI Activity
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "Exception during write");
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "close() of connect socket failed");
            }
        }
    }
}