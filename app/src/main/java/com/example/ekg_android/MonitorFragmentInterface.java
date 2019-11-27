package com.example.ekg_android;

public interface MonitorFragmentInterface {

    // Allows the monitor fragment to get the parent to display a notification on request
    public void onDisplayNotification (Sample sample);
}
