package com.cashin.nest.demo.services.NestService;

import com.cashin.nest.demo.data.enums.PurchaseActions;
import com.cashin.nest.demo.utils.HandleNestPurchaseHelper;
import com.cashin.nest.demo.services.NestService.base.CommunicationListener;
import com.cashin.nest.demo.services.NestService.base.CommunicationService;
import com.cashin.nest.demo.data.constants.CommunicationConstants;
import com.cashin.nest.demo.data.models.requests.PurchaseRequest;
import com.google.gson.Gson;

import timber.log.Timber;

public class NestService {
    private CommunicationService communicationService;
    private CommunicationListener communicationListener;
    private HandleNestPurchaseHelper.NestPurchaseResponseHandlerCallBack purchaseCallBack;

    public static NestService getInstance(CommunicationService communicationService, HandleNestPurchaseHelper.NestPurchaseResponseHandlerCallBack purchaseCallBack) {
        NestService instance = new NestService(communicationService);
        instance.purchaseCallBack = purchaseCallBack;
        return instance;
    }

    private NestService(CommunicationService communicationService) {
        this.communicationService = communicationService;
        this.communicationListener = new CommunicationListener() {
            @Override
            public void onConnected() {
                // Handle connected logic
                purchaseCallBack.setStatus(CommunicationConstants.STATE_CONNECTED);
            }

            @Override
            public void onDisconnected() {
                // Handle disconnected logic
                purchaseCallBack.setStatus(CommunicationConstants.STATE_NONE);
            }

            @Override
            public void onMessageReceived(String message) {
                // Handle message received
                HandleNestPurchaseHelper.handleResponse(message, purchaseCallBack);
            }
        };
        this.communicationService.setListener(this.communicationListener);
    }

    public void initiateConnection() {
        communicationService.connect();
    }

    public void disconnect() {
        communicationService.disconnect();
    }

    public boolean pay(String uuid, String phone, String name, double amountToPay, int nestPaymentType) {
        // Implement payment logic using communicationService
        try {
            PurchaseRequest request = new PurchaseRequest(PurchaseActions.Payment.getType(),uuid, phone, name, amountToPay, nestPaymentType);
            communicationService.send(new Gson().toJson(request));
            return true;
        }catch (Exception e){
            Timber.e(e);
            return false;
        }
    }

}
