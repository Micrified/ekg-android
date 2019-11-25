package com.example.ekg_android;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;


public class DataManager {

    // Internal reference to singleton class instance
    private static volatile DataManager singleton = new DataManager();

    // Synchronized event queue (for incoming events)
    private ArrayBlockingQueue samples;

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

    // Configuration settings
    private Comparator cfg_comp = Comparator.GREATER_THAN;
    private int cfg_val= 0x0;

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

    // Sets the configuration
    public void setSettings (Comparator comparator, int threshold) {
        this.cfg_comp = comparator;
        this.cfg_val = threshold;
    }

    // Returns the Setting comparator type
    public Comparator getSettingComparator () {
        return this.cfg_comp;
    }

    // Returns the Settings threshold
    public int getSettingThreshold () {
        return this.cfg_val;
    }

    // Returns the sample count
    public int getSampleCount () {
        return this.samples.size();
    }

    // Allows samples to be added to the synchronous queue
    public void addSample (Sample s) {
        try {
            this.samples.put(s);
            for (DataManagerInterface subscriber : subscribers) {
                subscriber.onNewSample(s);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Allows samples to be peeked at from the synchronous queue
    public Sample peekSample () {
        Sample s = null;
        if (this.samples.size() > 0) {
            s = (Sample)samples.peek();
        }
        return s;
    }

    // Allows samples to be dequeued from the synchronous queue
    public Sample removeSample () {
        Sample s = null;

        if (this.samples.size() > 0) {
            try {
                s = (Sample)samples.take();

            } catch (InterruptedException e) {
                e.printStackTrace();
                s = null;
            }
        }

        return s;
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
        this.samples = new ArrayBlockingQueue<Sample>(100);
    }

    public static DataManager getInstance() {
        return singleton;
    }
}