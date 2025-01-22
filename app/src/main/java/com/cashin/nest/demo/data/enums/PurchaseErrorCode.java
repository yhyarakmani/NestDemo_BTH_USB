package com.cashin.nest.demo.data.enums;

public enum PurchaseErrorCode {
    None(0),
    NotInitiated(1),
    InvalidAmount(2),
    TransactionCancelled(3),
    MissingPackageName(4),
    MissingClientID(5),
    MissingCustomerReferenceNumber(6),
    PaymentMethodIsNotOnboarded(7),
    InvalidCustomerPhone(8),
    InvalidCustomerName(9),
    InvalidCustomerEmail(10),
    ;
    private final int mType;

    PurchaseErrorCode(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

}
