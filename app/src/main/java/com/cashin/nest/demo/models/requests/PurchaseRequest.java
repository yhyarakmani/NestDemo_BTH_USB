package com.cashin.nest.demo.models.requests;

public class PurchaseRequest {
    public int action;
    public int paymentMethod;
    public String packageName;

    public long amount;
    public String customerReferenceNumber;
    boolean isFromBluetooth;
    boolean isFromUSB;
    public String customerPhone;
    public String customerName;
    public String customerEmail;

    public int getAction() {
        return action;
    }

    public int getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(int paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getCustomerReferenceNumber() {
        return customerReferenceNumber;
    }

    public void setCustomerReferenceNumber(String customerReferenceNumber) {
        this.customerReferenceNumber = customerReferenceNumber;
    }

    public boolean isFromBluetooth() {
        return isFromBluetooth;
    }

    public void setFromBluetooth(boolean fromBluetooth) {
        isFromBluetooth = fromBluetooth;
    }

    public boolean isFromUSB() {
        return isFromUSB;
    }

    public void setFromUSB(boolean fromUSB) {
        isFromUSB = fromUSB;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public void setAction(int action) {
        this.action = action;
    }
}


