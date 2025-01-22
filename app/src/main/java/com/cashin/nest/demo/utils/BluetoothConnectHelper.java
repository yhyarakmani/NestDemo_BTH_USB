package com.cashin.nest.demo.utils;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothConnectHelper {
    private final Activity activity;
    private final BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> pairedDevicesList = new ArrayList<>();
    private final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher;

    public BluetoothConnectHelper(Activity activity, ActivityResultLauncher<Intent> enableBluetoothLauncher) {
        this.activity = activity;
        this.enableBluetoothLauncher = enableBluetoothLauncher;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void initialize() {
        checkBluetoothSupport();
    }

    private void checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            AppHelper.showToast(activity, "Bluetooth is not supported on this device");
            activity.finish();
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            AppHelper.showToast(activity, "Bluetooth is already enabled");
        }
    }

    public void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN
            };

                ActivityCompat.requestPermissions(activity, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            // For Android 11 and below
            String[] permissions = {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

                ActivityCompat.requestPermissions(activity, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                enableBluetooth();
                AppHelper.showToast(activity, "Bluetooth permissions granted");
            } else {
                AppHelper.showToast(activity, "Bluetooth permissions denied. App functionality may be limited.");
            }
        }
    }

    public void listPairedDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            AppHelper.showToast(activity, "Enable Bluetooth first");
            return;
        }

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            AppHelper.showToast(activity, "Bluetooth Permissions is Required");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDevicesList.clear();

        if (!pairedDevices.isEmpty()) {
            pairedDevicesList.addAll(pairedDevices);
        } else {
            AppHelper.showToast(activity, "No paired devices found");
        }
    }

    public ArrayList<BluetoothDevice> getPairedDevicesList() {
        return pairedDevicesList;
    }
}