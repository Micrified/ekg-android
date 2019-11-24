package com.example.ekg_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MonitorFragment extends Fragment implements DataManagerInterface {

    // Listview
    private ListView listview_events;

    // List of events
    private ArrayList<Event> events;


    // Adapter for the listview
    private BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return events.size();
        }

        @Override
        public Object getItem(int i) {
            return events.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.event_cell, viewGroup, false);
            }

            // Extract event
            Event event = events.get(i);

            // Configure the timestamp textview
            TextView textview_timestamp = (TextView)view.findViewById(R.id.textview_timestamp);
            SimpleDateFormat format = new SimpleDateFormat("EEE, MMM d, 'at' HH:mm:ss");
            String timeString = format.format(event.getTimestamp());
            textview_timestamp.setText(timeString);

            // Configure the label textview
            TextView textview_label = (TextView)view.findViewById(R.id.textview_label);
            textview_label.setText(event.getSample().getLabel().toString());

            return view;
        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_monitor, container, false);

        // Initialize data structures (20 event limit)
        this.events = new ArrayList<Event>(20);

        // Connect views
        this.listview_events = root.findViewById(R.id.listview_events);

        // Connect the list view
        this.listview_events.setAdapter(this.adapter);

        // Subscribe to DataManager
        DataManager.getInstance().addSubscriber(this);

        return root;
    }

    @Override
    public void onNewSample(Sample sample) {

        // Ignore sample unless it is critical
        if (sample.getLabel() != Classification.ATRIAL &&
                sample.getLabel() != Classification.VENTRICAL) {
            return;
        }

        // Create a new arraylist with latest event at the front
        ArrayList<Event> newEvents = new ArrayList<Event>();
        newEvents.add(new Event(sample));
        newEvents.addAll(this.events);
        this.events = newEvents;

        // Refresh the listview
        this.adapter.notifyDataSetChanged();
    }
}
