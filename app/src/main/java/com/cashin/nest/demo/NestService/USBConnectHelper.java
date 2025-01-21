package com.cashin.nest.demo.NestService;

import static com.cashin.nest.demo.NestService.NestPurchaseUSBService.USB_TIMEOUT_IN_MS;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import java.util.HashMap;

public class USBConnectHelper {
    private UsbManager usbManager;
    private Context context;
    private UsbDevice device;
    private NestPurchaseUSBService service;
    public void connect(Context context,NestPurchaseUSBService service) {
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.context = context;
        this.service = service;
        if (service.isConnected) {
            return;
        }
        final HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList == null || deviceList.isEmpty()) {
            return;
        }
        for (UsbDevice device : deviceList.values()) {
            initAccessory(device);
        }
        searchForUsbAccessory(deviceList);
    }


    private void searchForUsbAccessory(final HashMap<String, UsbDevice> deviceList) {
        for (UsbDevice device : deviceList.values()) {
            if (isUsbAccessory(device) && usbManager.hasPermission(device)) {
                this.device = device;
                service.startCommunicationRunnable(this.device,this.usbManager);
                return;
            }
        }
    }

    private boolean isUsbAccessory(final UsbDevice device) {
        return (device.getProductId() == 0x2d00) || (device.getProductId() == 0x2d01);
    }

    private void initAccessory(final UsbDevice device) {
        //if (!usbManager.hasPermission(device)) {
        Intent intent = new Intent(context, this.getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_CANCEL_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, pendingIntent);
        //}
        final UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            return;
        }
        initStringControlTransfer(connection, 0,
                "CASHIN"); // MANUFACTURER
        initStringControlTransfer(connection, 1,
                "Android2AndroidAccessory"); // MODEL
        initStringControlTransfer(connection, 2,
                "USB communication"); // DESCRIPTION
        initStringControlTransfer(connection, 3,
                "1.0"); // VERSION
        initStringControlTransfer(connection, 4,
                "https://cashin.sa"); // URI
        initStringControlTransfer(connection, 5,
                "42"); // SERIAL
        connection.controlTransfer(0x40,
                53, 0, 0, new byte[]{},
                0, USB_TIMEOUT_IN_MS);
        connection.close();
    }

    private void initStringControlTransfer(final UsbDeviceConnection deviceConnection,
                                           final int index,
                                           final String string) {
        deviceConnection.controlTransfer(0x40, 52, 0, index,
                string.getBytes(), string.length(), USB_TIMEOUT_IN_MS);
    }
}
