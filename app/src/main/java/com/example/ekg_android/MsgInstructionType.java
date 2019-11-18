package com.example.ekg_android;

/*
    INST_EKG_SAMPLE = 0,        // Instruct device to sample EKG data
    INST_EKG_MONITOR,           // Instruct device to monitor user
    INST_EKG_IDLE,              // Instruct device to go into idle mode

    INST_TYPE_MAX               // Upper boundary value for instruction type
*/

public enum MsgInstructionType {
    INST_EKG_SAMPLE,
    INST_EKG_MONITOR,
    INST_EKG_IDLE
}
