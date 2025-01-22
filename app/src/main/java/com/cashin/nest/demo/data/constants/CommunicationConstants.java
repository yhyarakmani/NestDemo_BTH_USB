package com.cashin.nest.demo.data.constants;

import java.util.UUID;

public class CommunicationConstants {
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int BUFFER_SIZE_IN_BYTES = 1024;
    public static final int USB_TIMEOUT_IN_MS = 1000;
    public static final String END_OF_MESSAGE = "\n$";
    //Bluetooth
    public static final UUID MY_UUID_SECURE = UUID.fromString("af19f439-896b-48d1-b8cb-99c605a5543d");
    public static final String NAME_SECURE = "BluetoothSecure";

}
