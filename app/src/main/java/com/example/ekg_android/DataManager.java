package com.example.ekg_android;

import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;


public class DataManager {

    // Internal reference to singleton class instance
    private static volatile DataManager singleton = new DataManager();

    // Synchronized event queue (for incoming events)
    private ArrayList samples;

    // Array of subscribers to events
    ArrayList<DataManagerInterface> subscribers;

    // Training data constants
    public static final int max_count_a = 10;
    public static final int max_count_n = 20;
    public static final int max_count_v = 10;

    // Training data features
    ArrayList<Sample> training_data_a = new ArrayList<Sample>();
    ArrayList<Sample> training_data_n = new ArrayList<Sample>();
    ArrayList<Sample> training_data_v = new ArrayList<Sample>();

    // Easy Access Methods
    public int getCount_A () {
        return this.training_data_a.size();
    }
    public int getCount_N () {
        return this.training_data_n.size();
    }
    public int getCount_V () {
        return this.training_data_v.size();
    }

    // Clear Method for the training data
    public void clearTrainingData () {
        this.training_data_a.clear();
        this.training_data_n.clear();
        this.training_data_v.clear();
    }

    // Allows samples to be added to the synchronous queue
    public void addSample (Sample s) {
        this.samples.add(s);
        for (DataManagerInterface subscriber : subscribers) {
            subscriber.onNewSample(s);
        }
    }

    // Allows a subscriber to be added
    public void addSubscriber (DataManagerInterface subscriber) {
        this.subscribers.add(subscriber);
    }

    // Insertion methods
    public void addSample_A (Sample s) {
        training_data_a.add(s);
    }
    public void addSample_N (Sample s) {
        training_data_n.add(s);
    }
    public void addSample_V (Sample s) {
        training_data_v.add(s);
    }

    // Private Constructor
    private DataManager(){
        this.subscribers = new ArrayList<DataManagerInterface>();
        this.samples = new ArrayList<Sample>();
    }

    public static DataManager getInstance() {
        return singleton;
    }
}