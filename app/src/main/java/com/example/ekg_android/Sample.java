package com.example.ekg_android;

public class Sample {
    private int amplitude;
    private int period;

    public int getAmplitude() {
        return amplitude;
    }

    public int getPeriod() {
        return period;
    }

    public Sample (int amplitude, int period) {
        this.amplitude = amplitude;
        this.period = period;
    }
}
