package com.cashin.nest.demo.enums;

public enum PurchaseActions {
    Payment(0),
    Cancel(1),
    GetAvailablePaymentMethods(2),
    CheckNestVersion(3),
    UsbHandShake(4),
    ;
    private final int mType;

    PurchaseActions(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

}
