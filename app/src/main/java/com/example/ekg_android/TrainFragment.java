package com.example.ekg_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class TrainFragment extends Fragment implements View.OnClickListener, DataManagerInterface {

    // Delegate
    TrainFragmentInterface delegate;

    // Textviews
    private TextView textview_n_count;
    private TextView textview_a_count;
    private TextView textview_v_count;
    private TextView textview_sample_count;

    // Buttons
    private Button button_upload;
    private Button button_clear;

    // Listview
    private ListView listview_samples;

    // Listview Adapter
    private BaseAdapter listviewAdapter = new BaseAdapter() {

        // We only show one cell at a time, so MIN(1, count)
        @Override
        public int getCount() {
            return Math.min(1, DataManager.getInstance().getSampleCount());
        }

        // Always display the latest item
        @Override
        public Object getItem(int i) {
            return DataManager.getInstance().peekSample();
        }

        // We just return the index as the item identifier
        @Override
        public long getItemId(int i) {
            return i;
        }

        // Here we configure the view
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.train_cell, viewGroup, false);
            }

            // Extract sample
            Sample sample = DataManager.getInstance().peekSample();

            // If no sample, return null
            if (sample == null) {
                return null;
            }

            // Assign the Amplitude and R-R Period for the sample cell
            TextView textview_sample_amplitude = (TextView)view.findViewById(R.id.textview_sample_amplitude);
            TextView textview_sample_period    = (TextView)view.findViewById(R.id.textview_sample_period);

            // Assign the buttons
            Button button_label_normal    = (Button)view.findViewById(R.id.button_label_normal);
            Button button_label_atrial    = (Button)view.findViewById(R.id.button_label_atrial);
            Button button_label_ventrical = (Button)view.findViewById(R.id.button_label_ventrical);
            Button button_label_discard   = (Button)view.findViewById(R.id.button_discard);

            // Configure the textviews
            float amplitude = ((float)sample.getAmplitude() / 1000);
            textview_sample_amplitude.setText(String.format("%3fV", amplitude));
            textview_sample_period.setText(String.format("%dms", sample.getPeriod()));

            // Set the button listeners
            button_label_normal.setOnClickListener(TrainFragment.this);
            button_label_atrial.setOnClickListener(TrainFragment.this);
            button_label_ventrical.setOnClickListener(TrainFragment.this);
            button_label_discard.setOnClickListener(TrainFragment.this);

            return view;
        }
    };


    // Metadata
    private Sample pendingSample = null;


    // Updates a counter textview
    private void setTextViewCount (TextView textView, int count, int maximum) {
        int color = ContextCompat.getColor(getContext(), R.color.progressColor);
        int display_count = Math.min(maximum, count);
        textView.setText(String.format("%d/%d", display_count, maximum));
        if (display_count == maximum) {
            color = ContextCompat.getColor(getContext(), R.color.completeColor);
        }
        textView.setTextColor(color);
    }


    @Override
    public void onClick(View view) {
        DataManager m = DataManager.getInstance();
        switch (view.getId()) {
            case R.id.button_upload:
                System.out.println("Uploading");
                delegate.onUploadTrainingData();
                break;
            case R.id.button_clear:
                System.out.println("Clearing");
                m.clearTrainingData();
                this.pendingSample = null;
                break;

            case R.id.button_label_normal: {
                onSampleLabel(Classification.NORMAL);
            }
            break;
            case R.id.button_label_atrial: {
                onSampleLabel(Classification.ATRIAL);
            }
            break;
            case R.id.button_label_ventrical: {
                onSampleLabel(Classification.VENTRICAL);
            }
            break;
            case R.id.button_discard: {
                onSampleLabel(Classification.NONE);
            }
            break;
        }
        refresh();
    }

    // Refreshes the UI
    private void refresh () {
        DataManager m = DataManager.getInstance();
        boolean ready = (m.getCount_A() == m.max_count_a &&
                         m.getCount_N() == m.max_count_n &&
                         m.getCount_V() == m.max_count_v );

        // Update the progress buttons
        setTextViewCount(textview_a_count, m.getCount_A(), m.max_count_a);
        setTextViewCount(textview_n_count, m.getCount_N(), m.max_count_n);
        setTextViewCount(textview_v_count, m.getCount_V(), m.max_count_v);

        // Adjust available buttons based on whether all samples are ready
        if (ready) {
            button_clear.setEnabled(true);
            button_upload.setEnabled(true);
        } else {
            button_clear.setEnabled(true);
            button_upload.setEnabled(false);
        }

        // Order the listview to refresh
        this.listviewAdapter.notifyDataSetChanged();

        // Update the sample count textview
        this.textview_sample_count.setText(String.format("%2d Samples Available", m.getSampleCount()));
    }


    // Constructor
    public TrainFragment (TrainFragmentInterface delegate) {
        this.delegate = delegate;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_train, container, false);

        // Connect views
        this.textview_a_count = root.findViewById(R.id.textview_a);
        this.textview_n_count = root.findViewById(R.id.textview_n);
        this.textview_v_count = root.findViewById(R.id.textview_v);
        this.textview_sample_count = root.findViewById(R.id.textview_sample_count);

        this.button_upload = root.findViewById(R.id.button_upload);
        this.button_clear = root.findViewById(R.id.button_clear);

        // Setup the listview and assign it the adapter
        this.listview_samples = root.findViewById(R.id.listview_samples);
        this.listview_samples.setAdapter(this.listviewAdapter);

        // Subscribe to the DataManager
        DataManager.getInstance().addSubscriber(this);

        // Set listeners
        this.button_upload.setOnClickListener(this);
        this.button_clear.setOnClickListener(this);

        // Refresh UI
        refresh();

        return root;
    }


    public void onSampleLabel(Classification classification) {
        DataManager m = DataManager.getInstance();

        // Remove the sample from the queue
        Sample sample = DataManager.getInstance().removeSample();

        switch (classification) {
            case NONE:
                System.out.println("Discarding sample ...");
                break;
            case NORMAL:
                if (m.getCount_N() >= m.max_count_n) {
                    showAlert("Discarding sample (N): At capacity!");
                }
                m.addSample_N(sample);
                break;
            case ATRIAL:
                if (m.getCount_A() >= m.max_count_a) {
                    showAlert("Discarding sample (A): At capacity!");
                }
                m.addSample_A(sample);
                break;
            case VENTRICAL:
                if (m.getCount_V() >= m.max_count_v) {
                    showAlert("Discarding sample (V): At capacity!");
                }
                m.addSample_V(sample);
                break;
        }
    }

    private void showAlert (String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onNewSample(Sample sample) {

        // We just update the listview data when new samples become available
        this.listviewAdapter.notifyDataSetChanged();

        // Update the sample count textview
        this.textview_sample_count.setText(String.format("%2d Samples Available",
                DataManager.getInstance().getSampleCount()));
    }
}


