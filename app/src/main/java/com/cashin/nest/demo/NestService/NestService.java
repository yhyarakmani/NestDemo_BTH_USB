package com.cashin.nest.demo.NestService;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import timber.log.Timber;

public class NestService {

    public static NestService instance;
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTED = 3;

    private final NestPurchaseBluetoothService bluetoothService;
    private final BluetoothDevice bluetoothDevice;

    private final UseCaseCallback<Throwable, String, String> callback = new UseCaseCallback<>() {
        @Override
        public void onStatusChangedResponse(String statusString, int status) {
            if (purchaseCallBack != null) {
                purchaseCallBack.showMessage(statusString);
                purchaseCallBack.setStatus(status);
            }
        }

        @Override
        public void onFailureResponse(Throwable throwable) {
            Timber.e(throwable);
        }

        @Override
        public void onListenerInvoked(String message) {
            if (purchaseCallBack != null)
                NestPurchaseResponseHelper.handleResponse(message, purchaseCallBack);
        }
    };

    private NestPurchaseResponseHelper.NestPurchaseResponseHandlerCallBack purchaseCallBack;

    private NestService(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        this.bluetoothService = new NestPurchaseBluetoothService(callback);
    }

    public static NestService getInstance(BluetoothDevice bluetoothDevice, NestPurchaseResponseHelper.NestPurchaseResponseHandlerCallBack purchaseCallBack) {
        if (instance == null) {
            instance = new NestService(bluetoothDevice);
        }
        instance.purchaseCallBack = purchaseCallBack;
        return instance;
    }

    public void initiateConnection() {
        try {
            bluetoothService.connect(bluetoothDevice, true);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void checkVersion(ActivityResultLauncher<Intent> launcherNestPurchaseActions) {
        try {
            bluetoothService.checkVersion();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public boolean pay(String uuid, String phone, String name, double amountToPay, int nestPaymentType) {
        try {
            return bluetoothService.pay( uuid, phone, name, amountToPay, nestPaymentType);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    public void getAvailablePaymentMethods() {
        try {
            bluetoothService.getAvailablePaymentMethods();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void disconnect() {
        bluetoothService.stop();
    }

    public interface UseCaseCallback<GFailure, GResponse, GListener> {
        void onStatusChangedResponse(GResponse response, int status);

        void onFailureResponse(GFailure failure);

        void onListenerInvoked(GListener listener);
    }
}
