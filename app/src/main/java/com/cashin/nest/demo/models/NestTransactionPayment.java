package com.cashin.nest.demo.models;

public class NestTransactionPayment {

    private int id = 0;
    private double amount = 0.0;
    private int payment_method = 0;
    private int status = 0;

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public int getPayment_method() {
        return payment_method;
    }

    public int getStatus() {
        return status;
    }
}
