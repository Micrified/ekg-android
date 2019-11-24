package com.example.ekg_android;

public interface DataManagerInterface {

    // The DataManager will call delegates who sign up under this protocol when new samples arrive
    public void onNewSample (Sample sample);
}
