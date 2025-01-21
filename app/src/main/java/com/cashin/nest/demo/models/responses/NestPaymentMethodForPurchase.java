package com.cashin.nest.demo.models.responses;


public class NestPaymentMethodForPurchase {
    String arabic_name;
    String english_name;
    String icon;
    int type;

    public String getArabic_name() {
        return arabic_name;
    }

    public String getEnglish_name() {
        return english_name;
    }

    public String getIcon() {
        return icon;
    }

    public int getType() {
        return type;
    }
}
