package com.example.ekg_android;

public class Sample {
    private Classification label;
    private int amplitude;
    private int period;

    public Classification getLabel () {
        return this.label;
    }

    public int getAmplitude() {
        return amplitude;
    }

    public int getPeriod() {
        return period;
    }

    public Sample (Classification label, int amplitude, int period) {
        this.label = label;
        this.amplitude = amplitude;
        this.period = period;
    }
}
