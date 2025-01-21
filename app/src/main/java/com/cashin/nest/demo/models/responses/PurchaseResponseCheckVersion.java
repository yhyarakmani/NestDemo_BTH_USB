package com.cashin.nest.demo.models.responses;

public class PurchaseResponseCheckVersion extends PurchaseResponse{
    int versionCode;

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }
}
