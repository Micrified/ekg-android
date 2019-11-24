package com.example.ekg_android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.sql.Timestamp;

public class Event {
    private Sample sample;
    private Timestamp timestamp;

    public Sample getSample() {
        return sample;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }


    public Event (Sample sample) {
        this.sample = sample;
        java.util.Date date = new java.util.Date();
        this.timestamp = new Timestamp(date.getTime());
    }
}
