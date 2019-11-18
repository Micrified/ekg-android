package com.example.ekg_android;

/*
	MSG_TYPE_STATUS = 0,        // Message contains status bit-field only
    MSG_TYPE_TRAIN_DATA,        // Message containing all training data
    MSG_TYPE_SAMPLE_DATA,       // Message containing a data sample
    MSG_TYPE_INSTRUCTION,       // Message contains a device instruction

    MSG_TYPE_MAX                // Upper boundary value for the message type
 */


public enum MsgType {
    MSG_TYPE_STATUS((byte)0), MSG_TYPE_TRAIN_DATA((byte)1), MSG_TYPE_SAMPLE_DATA((byte)2), MSG_TYPE_INSTRUCTION_DATA((byte)3), MSG_TYPE_MAX((byte)4);

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


