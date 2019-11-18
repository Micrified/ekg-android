package com.example.ekg_android;

public class DeviceBluetoothException extends Exception {

    public DeviceBluetoothException (String msg, Throwable err) {
        super(msg,err);
    }

    public DeviceBluetoothException (String msg) {
        super(msg);
    }
}
