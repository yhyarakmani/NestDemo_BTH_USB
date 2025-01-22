package com.cashin.nest.demo;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cashin.nest.demo.services.NestService.BluetoothCommunicationService;
import com.cashin.nest.demo.services.NestService.NestService;
import com.cashin.nest.demo.utils.AppHelper;
import com.cashin.nest.demo.utils.HandleNestPurchaseHelper;
import com.cashin.nest.demo.data.enums.PurchasePaymentMethod;
import com.cashin.nest.demo.data.models.responses.PurchaseResponse;
import com.google.gson.Gson;

import java.util.UUID;

public class ConnectionActivity extends AppCompatActivity {
    private NestService nestService;
    private TextView textViewStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);
        Button buttonSendData = findViewById(R.id.buttonSendData);
        textViewStatus = findViewById(R.id.textViewStatus);
        buttonSendData.setOnClickListener(v -> sendData());

        BluetoothDevice device = getIntent().getParcelableExtra("BLUETOOTH_DEVICE");
        if (device == null) {
            AppHelper.showToast(this, "No device selected");
            finish();
        }
        nestService = NestService.getInstance(new BluetoothCommunicationService(device), new HandleNestPurchaseHelper.NestPurchaseResponseHandlerCallBack() {
            @Override
            public void onSuccess(PurchaseResponse purchaseResponse) {
                AppHelper.showToast(ConnectionActivity.this, purchaseResponse.getMessage());
                runOnUiThread(() -> textViewStatus.setText(new Gson().toJson(purchaseResponse)));

            }

            @Override
            public void showMessage(String message) {
                runOnUiThread(() -> textViewStatus.setText(message));

            }

            @Override
            public void setStatus(int status) {
                runOnUiThread(() -> AppHelper.showToast(ConnectionActivity.this, "Status: " + status));
            }

        });
        nestService.initiateConnection();
    }

    private void sendData() {
       boolean res =  nestService.pay(UUID.randomUUID().toString(),"582582888","test",100.50, PurchasePaymentMethod.None.getType());
       if(res)
           textViewStatus.setText("Data sent successfully");
    }
}
