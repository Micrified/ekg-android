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

    // Getters
    public ArrayList<Sample> getNormalTrainingData () {
        return this.training_data_n;
    }
    public ArrayList<Sample> getAtrialTrainingData () {
        return this.training_data_a;
    }
    public ArrayList<Sample> getVentricalTrainingData () {
        return this.training_data_v;
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

    // Generates a training set
    public void applyDefaultTrainingSet () {

        // Clear the existing training data
        this.training_data_n.clear();
        this.training_data_a.clear();
        this.training_data_v.clear();

        // Default setpoint for normal heartbeats
        int n_amplitude = 2500;
        int n_period    = 1000;

        // Default setpoint for atrial heartbeats
        int a_amplitude = 2500;
        int a_period    = 500;

        // Default setpoint for ventrical heartbeats
        int v_amplitude = 800;
        int v_period    = 1000;

        // Add Normal (with noise)
        for (int i = 0; i < 20; ++i) {
            int noise = -5 + (int)(10.0 * Math.random());
            Sample n = new Sample(Classification.NORMAL, n_amplitude + noise, n_period + noise);
            this.addSample_N(n);
        }

        // Add Atrail (with noise)
        for (int i = 0; i < 10; ++i) {
            int noise = -5 + (int)(10.0 * Math.random());
            Sample a = new Sample(Classification.ATRIAL, a_amplitude + noise, a_period + noise);
            this.addSample_A(a);
        }

        // Add Ventrical (with noise)
        for (int i = 0; i < 10; ++i) {
            int noise = -5 + (int)(10.0 * Math.random());
            Sample v = new Sample(Classification.VENTRICAL, v_amplitude + noise, v_period + noise);
            this.addSample_V(v);
        }
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