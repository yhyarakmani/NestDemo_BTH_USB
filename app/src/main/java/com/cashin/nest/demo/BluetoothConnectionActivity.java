package com.cashin.nest.demo;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cashin.nest.demo.NestService.NestBluetoothService;
import com.cashin.nest.demo.NestService.NestPurchaseResponseHelper;
import com.cashin.nest.demo.enums.PurchasePaymentMethod;
import com.cashin.nest.demo.models.responses.PurchaseResponse;
import com.google.gson.Gson;

import java.util.UUID;

public class BluetoothConnectionActivity extends AppCompatActivity {
    private NestBluetoothService bluetoothService;
    private BluetoothDevice device;

    private TextView textViewStatus;
    private Button buttonSendData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        device = getIntent().getParcelableExtra("DEVICE");
        if (device == null) {
            Toast.makeText(this, "No device selected", Toast.LENGTH_SHORT).show();
            finish();
        }

        textViewStatus = findViewById(R.id.textViewStatus);
        buttonSendData = findViewById(R.id.buttonSendData);

        bluetoothService = NestBluetoothService.getInstance(device, new NestPurchaseResponseHelper.NestPurchaseResponseHandlerCallBack() {
            @Override
            public void onSuccess(PurchaseResponse purchaseResponse) {
                Toast.makeText(BluetoothConnectionActivity.this, purchaseResponse.getMessage(), Toast.LENGTH_SHORT).show();
                runOnUiThread(() -> textViewStatus.setText(new Gson().toJson(purchaseResponse)));

            }

            @Override
            public void showMessage(String message) {
                runOnUiThread(() -> textViewStatus.setText(message));

            }

            @Override
            public void setStatus(int status) {
                runOnUiThread(() -> Toast.makeText(BluetoothConnectionActivity.this, "Status: " + status, Toast.LENGTH_SHORT).show());
            }

        });

        bluetoothService.initiateConnection();

        buttonSendData.setOnClickListener(v -> sendData());
    }

    private void sendData() {
        bluetoothService.pay(UUID.randomUUID().toString(),"582582888","test",100.50, PurchasePaymentMethod.None.getType());
    }
}
