package com.example.ekg_android;

public enum MsgType {
    MSG_TYPE_STATUS((byte)0), MSG_TYPE_WIFI_DATA((byte)1), MSG_TYPE_STREAM_DATA((byte)2), MSG_TYPE_TELEMETRY_DATA((byte)3), MSG_TYPE_INSTRUCTION_DATA((byte)4), MSG_TYPE_MAX((byte)5);

    public byte byteVal;

    MsgType(byte byteVal) {
        this.byteVal = byteVal;
    }

    public byte getByteValue() {
        return byteVal;
    }

    public static MsgType fromByte(byte b) {
        for (MsgType type : values()) {
            if (type.getByteValue() == b) {
                return type;
            }
        }
        return null;
    }
}

