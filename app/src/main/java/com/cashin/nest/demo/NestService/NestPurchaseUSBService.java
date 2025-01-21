package com.cashin.nest.demo.NestService;

import static com.cashin.nest.demo.NestService.NestService.STATE_CONNECTED;
import static com.cashin.nest.demo.NestService.NestService.STATE_NONE;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import com.cashin.nest.demo.enums.PurchaseActions;
import com.cashin.nest.demo.models.requests.PurchaseRequest;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class NestPurchaseUSBService {
    private UsbManager usbManager;
    NestService.UseCaseCallback<Throwable, String, String> callback;
    public static final int USB_TIMEOUT_IN_MS = 1000;
    public static final int BUFFER_SIZE_IN_BYTES = 1024;
    private final AtomicBoolean keepThreadAlive = new AtomicBoolean(true);
    private final List<String> sendBuffer = new ArrayList<>();
    public static final String END_OF_MESSAGE = "\n$";
    UsbDevice device;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;

    boolean isConnected = false;
    private final Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if (msg.arg1 == STATE_CONNECTED) {
                    callback.onStatusChangedResponse( "Conntected to Nest", STATE_CONNECTED);
                }
                if (msg.arg1 == STATE_NONE) {
                    callback.onStatusChangedResponse("Connection failed", STATE_NONE);
                }
                break;
            case MESSAGE_READ:
                callback.onListenerInvoked(msg.obj.toString());
                break;
        }
        return true;
    });

    public NestPurchaseUSBService( NestService.UseCaseCallback<Throwable, String, String> callback) {
        this.callback = callback;

    }

    protected void sendString(final String string) {
        sendBuffer.add(Helper.stringToUnicode(string + END_OF_MESSAGE));
    }

    public void checkVersion() {
        launchNestPurchaseAction( PurchaseActions.CheckNestVersion.getType(), null);
    }

    public void getAvailablePaymentMethods() {
        launchNestPurchaseAction( PurchaseActions.GetAvailablePaymentMethods.getType(), null);
    }

    public boolean pay( String uuid, String phone, String name, double amountToPay, int nestPaymentType) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        phone = phone.contains("966") ? phone.substring(3) : phone;
        purchaseRequest.setCustomerPhone(phone);
        purchaseRequest.setCustomerName(name);
        purchaseRequest.setCustomerReferenceNumber(uuid);
        purchaseRequest.setAmount((long) Helper.round(amountToPay * 100, 2));
        purchaseRequest.setPaymentMethod(nestPaymentType);

        return launchNestPurchaseAction( PurchaseActions.Payment.getType(), purchaseRequest);
    }

    private boolean launchNestPurchaseAction(int actionType, PurchaseRequest purchaseRequest) {
        try {
            // If purchaseRequest is null, create a new one and set base fields
            if (purchaseRequest == null) {
                purchaseRequest = new PurchaseRequest();
            }
            purchaseRequest.setAction(actionType);
            purchaseRequest.setPackageName("com.cashin.nestDemo");

            // Serialize the PurchaseRequest object to JSON
            Gson gson = new Gson();
            String requestJson = gson.toJson(purchaseRequest);
            sendString(requestJson);
            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    public void disconnect(UsbDevice detachedDevice) {
        if (detachedDevice != null && detachedDevice.equals(device)) {
            stop();
        }
    }

    public void stop() {
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_NONE, -1).sendToTarget();
        Timber.d("USB device detached");
        isConnected = false;
        keepThreadAlive.set(false); // Stop the communication thread
    }

    public void startCommunicationRunnable(UsbDevice usbDevice,UsbManager usbManager) {
        this.device = usbDevice;
        this.usbManager = usbManager;
        new Thread(new CommunicationRunnable()).start();
    }

    public class CommunicationRunnable implements Runnable {

        @Override
        public void run() {
            keepThreadAlive.set(true);

            UsbEndpoint endpointIn = null;
            UsbEndpoint endpointOut = null;

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

            if (!claimResult) {
                //printLineToUI("Could not claim device");
            } else {
                final byte buff[] = new byte[BUFFER_SIZE_IN_BYTES];
                isConnected = true;
                mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_CONNECTED, -1).sendToTarget();
                //printLineToUI("Claimed interface - ready to communicate");
                try {
                    StringBuilder messageBuffer = new StringBuilder();
                    while (keepThreadAlive.get()) {
                        Thread.sleep(10);

                        // Read from the endpoint
                        final int bytesTransferred =
                                connection.bulkTransfer(endpointIn, buff, buff.length, USB_TIMEOUT_IN_MS);

                        if (bytesTransferred > 0) {
                            // Append the received chunk to the messageBuffer
                            String chunk = new String(buff, 0, bytesTransferred);
                            messageBuffer.append(chunk);

                            // Check if a complete message is available
                            int delimiterIndex;
                            while ((delimiterIndex = messageBuffer.indexOf(END_OF_MESSAGE)) != -1) {
                                // Extract the complete message up to the delimiter
                                String completeMessage = messageBuffer.substring(0, delimiterIndex);

                                // Send the complete message via handler
                                mHandler.obtainMessage(MESSAGE_READ, completeMessage.length(), -1, completeMessage)
                                        .sendToTarget();

                                // Remove the processed message from the buffer
                                messageBuffer.delete(0, delimiterIndex + END_OF_MESSAGE.length());
                            }
                        }

                        // Send data if available in sendBuffer
                        synchronized (sendBuffer) {
                            if (!sendBuffer.isEmpty()) {
                                final byte[] sendBuff = sendBuffer.get(0).getBytes();
                                connection.bulkTransfer(endpointOut, sendBuff, sendBuff.length, USB_TIMEOUT_IN_MS);
                                sendBuffer.remove(0);
                            }
                        }
                    }
                } catch (Exception e) {
                    mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_NONE, -1).sendToTarget();
                    Timber.e(e);
                }
            }
            connection.releaseInterface(usbInterface);
            connection.close();
            keepThreadAlive.set(false);
            /*isConnected = false;
            connect();*/
        }
    }

}