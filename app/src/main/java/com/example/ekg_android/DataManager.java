package com.example.ekg_android;

import java.util.ArrayList;



public class DataManager {

    // Internal reference to singleton class instance
    private static volatile DataManager singleton = new DataManager();

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

    // Clear Method
    public void clear () {
        this.training_data_a.clear();
        this.training_data_n.clear();
        this.training_data_v.clear();
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
    private DataManager(){}

    public static DataManager getInstance() {
        return singleton;
    }
}