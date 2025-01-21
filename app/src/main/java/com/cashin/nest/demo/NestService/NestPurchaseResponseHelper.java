package com.cashin.nest.demo.NestService;


import android.content.Context;

import com.cashin.nest.demo.NestConstants;
import com.cashin.nest.demo.enums.PurchaseActions;
import com.cashin.nest.demo.enums.PurchaseErrorCode;
import com.cashin.nest.demo.models.responses.PurchaseResponse;
import com.cashin.nest.demo.models.responses.PurchaseResponseCheckVersion;
import com.cashin.nest.demo.models.responses.PurchaseResponsePaymentMethods;
import com.cashin.nest.demo.models.responses.PurchaseResponseTransaction;
import com.google.gson.Gson;

import timber.log.Timber;

public class NestPurchaseResponseHelper {
    public static void handleResponse(String response, NestPurchaseResponseHandlerCallBack callBack) {
        Timber.d("response from nest: %s", response);
        try {
            PurchaseResponse purchaseResponse = new Gson().fromJson(response, PurchaseResponse.class);
            if (purchaseResponse.getErrorCode() != PurchaseErrorCode.None.getType()) {
                callBack.showMessage(purchaseResponse.getMessage());
                return;
            }
            if (purchaseResponse.getAction() == PurchaseActions.CheckNestVersion.getType()) {
                PurchaseResponseCheckVersion purchaseResponseCheckVersion = new Gson().fromJson(response, PurchaseResponseCheckVersion.class);
                if (purchaseResponseCheckVersion.getVersionCode() < NestConstants.RequiredNestVersionCode) {
                    callBack.showMessage("Supported");
                } else {
                    callBack.showMessage("Not Supported");
                }
                callBack.onSuccess(purchaseResponseCheckVersion);
                return;
            }
            if (purchaseResponse.getAction() == PurchaseActions.GetAvailablePaymentMethods.getType()) {
                PurchaseResponsePaymentMethods purchaseResponsePaymentMethods = new Gson().fromJson(response, PurchaseResponsePaymentMethods.class);
                callBack.onSuccess(purchaseResponsePaymentMethods);
                return;
            }
            if (purchaseResponse.getAction() == PurchaseActions.Payment.getType()) {
                PurchaseResponseTransaction purchaseResponsePaymentMethods = new Gson().fromJson(response, PurchaseResponseTransaction.class);
                callBack.onSuccess(purchaseResponsePaymentMethods);
                return;
            }
            if (purchaseResponse.getAction() == PurchaseActions.UsbHandShake.getType()) {
                if (callBack instanceof NestPurchaseResponseHandlerWithHandshakeCallBack)
                    ((NestPurchaseResponseHandlerWithHandshakeCallBack) callBack).handShake();
                return;
            }
            callBack.onSuccess(purchaseResponse);
        } catch (Exception e) {
            callBack.onSuccess(null);
            Timber.e(e);
        }
    }

    public interface NestPurchaseResponseHandlerCallBack {
        void onSuccess(PurchaseResponse purchaseResponse);
        void showMessage(String message);
        void setStatus(int status);
    }

    public interface NestPurchaseResponseHandlerWithHandshakeCallBack extends NestPurchaseResponseHandlerCallBack {
        void handShake();
    }
}