package com.cashin.nest.demo;

import android.app.ComponentCaller;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cashin.nest.demo.utils.AppHelper;
import com.cashin.nest.demo.utils.BluetoothConnectHelper;
import com.cashin.nest.demo.utils.USBConnectHelper;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private BluetoothConnectHelper bluetoothHelper;
    // Register the ActivityResultLauncher for Bluetooth enable
    ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    AppHelper.showToast(this, "Bluetooth enabled successfully");
                } else {
                    AppHelper.showToast(this, "Bluetooth enabling failed");
                }
            }
    );
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize UI components
        ListView listViewDevices = findViewById(R.id.listViewDevices);
        Button initiateUSBDevice = findViewById(R.id.initiateUSBDevice);
        Button buttonRefreshDevices = findViewById(R.id.buttonRefreshDevices);
        Button requestBluetoothPermissions = findViewById(R.id.requestBluetoothPermissions);

        // Initialize Bluetooth helper with the launcher
        bluetoothHelper = new BluetoothConnectHelper(this, enableBluetoothLauncher);
        bluetoothHelper.initialize();

        // Initialize adapter
        ArrayAdapter<String> devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listViewDevices.setAdapter(devicesAdapter);

        // Set button listeners
        initiateUSBDevice.setOnClickListener(v -> connectToUsbDevice());
        requestBluetoothPermissions.setOnClickListener(v -> bluetoothHelper.requestBluetoothPermissions());
        buttonRefreshDevices.setOnClickListener(v -> {
            bluetoothHelper.listPairedDevices();
            if (!bluetoothHelper.getPairedDevicesList().isEmpty()) {
                for (BluetoothDevice device : bluetoothHelper.getPairedDevicesList()
                ) {
                    devicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        });

        // Set item click listener for list view
        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = bluetoothHelper.getPairedDevicesList().get(position);
            connectToBluetoothDevice(selectedDevice);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void connectToBluetoothDevice(BluetoothDevice device) {
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.putExtra("BLUETOOTH_DEVICE", device);
        startActivity(intent);
    }

    private void connectToUsbDevice() {
        if(USBConnectHelper.getInstance().getDevice()!=null){
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivity(intent);
        }else
            AppHelper.showToast(this, "No USB device attached");

    }

    @Override
    protected void onResume() {
        super.onResume();
        USBConnectHelper.getInstance().connect((UsbManager) getSystemService(Context.USB_SERVICE), this);
    }

}