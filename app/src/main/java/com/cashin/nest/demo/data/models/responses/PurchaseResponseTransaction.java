package com.cashin.nest.demo.data.models.responses;


import com.cashin.nest.demo.data.models.NestTransaction;

public class PurchaseResponseTransaction extends PurchaseResponse{
    private NestTransaction transaction;
    public NestTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(NestTransaction transaction) {
        this.transaction = transaction;
    }

}
