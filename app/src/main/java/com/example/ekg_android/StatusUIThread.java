package com.example.ekg_android;

public class StatusUIThread implements Runnable {
    public int color;
    public String text;

    public StatusUIThread (int color, String text) {
        this.color = color;
        this.text = text;
    }

    public void run() {
        // do whatever you want with data
    }
}