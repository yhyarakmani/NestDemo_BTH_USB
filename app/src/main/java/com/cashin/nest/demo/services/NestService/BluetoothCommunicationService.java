package com.cashin.nest.demo.services.NestService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.cashin.nest.demo.data.constants.CommunicationConstants;
import com.cashin.nest.demo.services.NestService.base.CommunicationListener;
import com.cashin.nest.demo.services.NestService.base.CommunicationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class BluetoothCommunicationService implements CommunicationService {
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothDevice device;
    private CommunicationListener listener;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private volatile boolean isRunning = false;

    public BluetoothCommunicationService(BluetoothDevice device) {
        this.device = device;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void connect() {
        isRunning = true;
        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device, true); // Use secure connection; change to false for insecure
        connectThread.start();
    }

    @Override
    public void disconnect() {
        isRunning = false;
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        listener.onDisconnected();
    }

    @Override
    public void send(String data) {
        if (connectedThread != null) {
            data = data + CommunicationConstants.END_OF_MESSAGE;
            connectedThread.write(data.getBytes());
        } else {
            Timber.e("Cannot send data: Not connected");
        }
    }

    @Override
    public void setListener(CommunicationListener listener) {
        this.listener = listener;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null) {
            acceptThread = new AcceptThread(); // Use secure connection; change to false for insecure
            acceptThread.start();
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     */
    private synchronized void connected(BluetoothSocket socket) {
        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want one connection
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Notify the listener that the connection has been established
        if (listener != null) {
            listener.onConnected();
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(CommunicationConstants.NAME_SECURE, CommunicationConstants.MY_UUID_SECURE);
            } catch (IOException e) {
                Timber.e(e, "listen() failed");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Timber.e("BEGIN mAcceptThread");
            setName("AcceptThread");
            BluetoothSocket socket;
            // Listen to the server socket if we're not connected
            while (isRunning) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Timber.e(e, "accept() failed");
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    connected(socket);
                }
            }
            Timber.i("END mAcceptThread");
        }

        public void cancel() {
            Timber.d("cancel %s", this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Timber.e(e, "close() of server failed");
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(CommunicationConstants.MY_UUID_SECURE);
            } catch (IOException e) {
                Timber.e(e, "create() failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            Timber.e("BEGIN mConnectThread");
            setName("ConnectThread");
            // Cancel discovery because it otherwise slows down the connection
            bluetoothAdapter.cancelDiscovery();
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
                    Timber.e(e2, "unable to close() during connection failure");
                }
                listener.onDisconnected();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothCommunicationService.this) {
                connectThread = null;
            }
            // Start the connected thread
            connected(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Timber.e(e, "close() of connect failed");
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

        public ConnectedThread(BluetoothSocket socket) {
            Timber.e("create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Timber.e(e, "temp sockets not created");
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Timber.i("BEGIN mConnectedThread");
            byte[] buffer = new byte[1024]; // Adjusted buffer size for chunks
            int bytes;
            StringBuilder fullMessage = new StringBuilder(); // Accumulate data here

            // Keep listening to the InputStream while connected
            while (isRunning) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Convert bytes to a string and accumulate in fullMessage
                    String chunk = new String(buffer, 0, bytes);
                    fullMessage.append(chunk);

                    // Check if the message is complete (e.g., contains a delimiter)
                    int delimiterIndex;
                    while ((delimiterIndex = fullMessage.indexOf(CommunicationConstants.END_OF_MESSAGE)) != -1) {
                        // Extract complete message
                        String completeMessage = fullMessage.substring(0, delimiterIndex);

                        // Remove the processed message from the buffer
                        fullMessage.delete(0, delimiterIndex + CommunicationConstants.END_OF_MESSAGE.length());

                        // Send the complete message to the UI Activity
                        if (listener != null) {
                            listener.onMessageReceived(completeMessage);
                        }
                    }
                } catch (IOException e) {
                    Timber.e(e, "disconnected");
                    listener.onDisconnected();
                    // Start the service over to restart listening mode
                    BluetoothCommunicationService.this.start();
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

            } catch (IOException e) {
                Timber.e(e, "Exception during write");
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Timber.e(e, "close() of connect socket failed");
            }
        }
    }

}