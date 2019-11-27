package com.example.ekg_android;

/*
    INST_EKG_STOP = 0,          // Instruct device to sample EKG data
    INST_EKG_START,             // Instruct device to monitor user
    INST_EKG_CONFIGURE,         // Instruct device to update configuration

    INST_TYPE_MAX               // Upper boundary value for instruction type
*/

public enum MsgInstructionType {
    INST_EKG_STOP,
    INST_EKG_START,
    INST_EKG_CONFIGURE
}
