package com.cashin.nest.demo.models.responses;


import com.cashin.nest.demo.models.NestTransaction;

public class PurchaseResponseTransaction extends PurchaseResponse{
    private NestTransaction transaction;
    public NestTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(NestTransaction transaction) {
        this.transaction = transaction;
    }

}
