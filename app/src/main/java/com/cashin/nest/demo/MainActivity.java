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

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> pairedDevicesList = new ArrayList<>();
    private ArrayAdapter<String> devicesAdapter;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ListView listViewDevices = findViewById(R.id.listViewDevices);
        Button buttonEnableBluetooth = findViewById(R.id.buttonEnableBluetooth);
        Button buttonRefreshDevices = findViewById(R.id.buttonRefreshDevices);
        Button requestBluetoothPermissions = findViewById(R.id.requestBluetoothPermissions);

        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listViewDevices.setAdapter(devicesAdapter);

        // Initialize the ActivityResultLauncher
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Bluetooth enabling failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        buttonEnableBluetooth.setOnClickListener(v -> enableBluetooth());
        buttonRefreshDevices.setOnClickListener(v -> listPairedDevices());
        requestBluetoothPermissions.setOnClickListener(v -> requestBluetoothPermissions());

        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = pairedDevicesList.get(position);
            connectToDevice(selectedDevice);
        });

        checkBluetoothSupport();
    }

    private void checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show();
        }
    }
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private void requestBluetoothPermissions() {
// Check if we need to request permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12 and above
            String[] permissions = {
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_ADMIN
            };

            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }
        } else {
            // For Android 11 and below
            String[] permissions = {
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };

            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Helper method to check if all permissions are granted
    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth permissions denied. App functionality may be limited.", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void listPairedDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth Permissions is Required", Toast.LENGTH_LONG).show();
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDevicesList.clear();
        devicesAdapter.clear();

        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesList.add(device);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth Permissions is Required", Toast.LENGTH_LONG).show();
                    return;
                }
                devicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        Intent intent = new Intent(this, BluetoothConnectionActivity.class);
        intent.putExtra("DEVICE", device);
        startActivity(intent);
    }

}