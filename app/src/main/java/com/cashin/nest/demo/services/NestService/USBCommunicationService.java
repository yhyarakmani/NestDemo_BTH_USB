package com.cashin.nest.demo.services.NestService;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.cashin.nest.demo.utils.AppHelper;
import com.cashin.nest.demo.services.NestService.base.CommunicationListener;
import com.cashin.nest.demo.services.NestService.base.CommunicationService;
import com.cashin.nest.demo.data.constants.CommunicationConstants;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class USBCommunicationService implements CommunicationService {
    private final UsbManager usbManager;
    private final List<String> sendBuffer = new ArrayList<>();
    private final UsbDevice device;
    private final UsbDeviceConnection connection;
    private CommunicationListener listener;
    private Thread communicationThread;
    private volatile boolean isRunning = false;

    public USBCommunicationService(UsbManager usbManager,UsbDevice device,UsbDeviceConnection connection) {
        this.usbManager = usbManager;
        this.device = device;
        this.connection = connection;
    }
    @Override
    public void setListener(CommunicationListener listener) {
        this.listener = listener;
    }
    @Override
    public void connect() {
        isRunning = true;
        communicationThread = new Thread(this::communicate);
        communicationThread.start();
    }

    @Override
    public void disconnect() {
        isRunning = false;
        if (connection != null) {
            connection.close();
        }
        if (communicationThread != null) {
            communicationThread.interrupt();
        }
        listener.onDisconnected();
    }

    @Override
    public void send(String data) {
        sendBuffer.add(AppHelper.stringToUnicode(data + CommunicationConstants.END_OF_MESSAGE));
    }



    private void communicate() {
        UsbEndpoint endpointIn = null;
        UsbEndpoint endpointOut = null;

        if(device==null || device.getInterfaceCount()==0)
            return;
        final UsbInterface usbInterface = device.getInterface(0);

        for (int i = 0; i < device.getInterface(0).getEndpointCount(); i++) {

            final UsbEndpoint endpoint =
                    device.getInterface(0).getEndpoint(i);
            if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                endpointIn = endpoint;
            }
            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                endpointOut = endpoint;
            }

        }

        if (endpointIn == null) {
            return;
        }

        if (endpointOut == null) {
            return;
        }

        final UsbDeviceConnection connection = usbManager.openDevice(device);

        if (connection == null) {
            return;
        }

        final boolean claimResult = connection.claimInterface(usbInterface, true);

        if (claimResult) {
            final byte[] buff = new byte[CommunicationConstants.BUFFER_SIZE_IN_BYTES];
            isRunning = true;
            listener.onConnected();
            try {
                StringBuilder messageBuffer = new StringBuilder();
                while (isRunning) {
                    Thread.sleep(10);
                    final int bytesTransferred =
                            connection.bulkTransfer(endpointIn, buff, buff.length, CommunicationConstants.USB_TIMEOUT_IN_MS);

                    if (bytesTransferred > 0) {
                        String chunk = new String(buff, 0, bytesTransferred);
                        messageBuffer.append(chunk);
                        // Check if a complete message is available
                        int delimiterIndex;
                        while ((delimiterIndex = messageBuffer.indexOf(CommunicationConstants.END_OF_MESSAGE)) != -1) {
                            String completeMessage = messageBuffer.substring(0, delimiterIndex);
                            listener.onMessageReceived(completeMessage);
                            messageBuffer.delete(0, delimiterIndex + CommunicationConstants.END_OF_MESSAGE.length());
                        }
                    }
                    // Send data if available in sendBuffer
                    synchronized (sendBuffer) {
                        if (!sendBuffer.isEmpty()) {
                            final byte[] sendBuff = sendBuffer.get(0).getBytes();
                            connection.bulkTransfer(endpointOut, sendBuff, sendBuff.length, CommunicationConstants.USB_TIMEOUT_IN_MS);
                            sendBuffer.remove(0);
                        }
                    }
                }
            } catch (Exception e) {
                listener.onDisconnected();
                Timber.e(e);
            }
        }
        connection.releaseInterface(usbInterface);
        disconnect();
    }
}
