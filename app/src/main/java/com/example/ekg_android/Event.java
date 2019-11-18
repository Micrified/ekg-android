package com.example.ekg_android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.sql.Timestamp;

public class Event {
    private Sample sample;
    private Timestamp timestamp;
    private Classification classification;

    public Sample getSample() {
        return sample;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public Classification getClassification() {
        return classification;
    }

    public Event (Sample sample, Classification classification) {
        this.sample = sample;
        java.util.Date date = new java.util.Date();
        this.timestamp = new Timestamp(date.getTime());
        this.classification = classification;
    }
}
