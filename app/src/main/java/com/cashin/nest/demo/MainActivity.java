package com.cashin.nest.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cashin.nest.demo.utils.AppHelper;
import com.cashin.nest.demo.utils.BluetoothConnectHelper;

import java.util.ArrayList;
import java.util.Set;

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
        Button buttonRefreshDevices = findViewById(R.id.buttonRefreshDevices);
        Button requestBluetoothPermissions = findViewById(R.id.requestBluetoothPermissions);

        // Initialize Bluetooth helper with the launcher
        bluetoothHelper = new BluetoothConnectHelper(this, enableBluetoothLauncher);
        bluetoothHelper.initialize();

        // Initialize adapter
        ArrayAdapter<String> devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listViewDevices.setAdapter(devicesAdapter);

        // Set button listeners
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
            connectToDevice(selectedDevice);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void connectToDevice(BluetoothDevice device) {
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.putExtra("BLUETOOTH_DEVICE", device);
        startActivity(intent);
    }

}