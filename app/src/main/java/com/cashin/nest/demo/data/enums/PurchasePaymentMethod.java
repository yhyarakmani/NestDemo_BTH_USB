package com.cashin.nest.demo.data.enums;

public enum PurchasePaymentMethod {
    None(0),
    CashinPay(1),
    CashinLink(2),
    Tamara(3),
    Tabby(4),
    Madfu(5),
    STC(6),
    ;
    private final int mType;

    PurchasePaymentMethod(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

}
