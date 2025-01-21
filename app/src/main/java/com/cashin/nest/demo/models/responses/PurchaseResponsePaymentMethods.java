package com.cashin.nest.demo.models.responses;

import java.util.List;

public class PurchaseResponsePaymentMethods extends PurchaseResponse{
    private List<NestPaymentMethodForPurchase> paymentMethods;

    public List<NestPaymentMethodForPurchase> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<NestPaymentMethodForPurchase> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }
}
