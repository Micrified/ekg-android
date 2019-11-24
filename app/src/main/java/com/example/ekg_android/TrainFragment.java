package com.example.ekg_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class TrainFragment extends Fragment implements View.OnClickListener, LabelDialog.LabelDialogListener {

    // Textviews
    private TextView textview_n_count;
    private TextView textview_a_count;
    private TextView textview_v_count;

    // Buttons
    private Button button_upload;
    private Button button_clear;
    private Button button_sample;


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

    // Handles the arrival of a sample
    private void onNewSample (int amplitude, int period) {
        this.pendingSample = new Sample(Classification.NONE, amplitude, period);
        LabelDialog labelDialog = new LabelDialog(this.pendingSample, this);
        labelDialog.show(getFragmentManager(), "Classify");
    }


    @Override
    public void onClick(View view) {
        DataManager m = DataManager.getInstance();
        switch (view.getId()) {
            case R.id.button_upload:
                System.out.println("Uploading");
                break;
            case R.id.button_clear:
                System.out.println("Clearing");
                m.clearTrainingData();
                this.pendingSample = null;
                break;
            case R.id.button_sample:
                System.out.println("Sampling");
                this.onNewSample(30,25);
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
            button_sample.setEnabled(false);
        } else {
            button_clear.setEnabled(true);
            button_upload.setEnabled(false);
            button_sample.setEnabled(true);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_train, container, false);

        // Connect views
        this.textview_a_count = root.findViewById(R.id.textview_a);
        this.textview_n_count = root.findViewById(R.id.textview_n);
        this.textview_v_count = root.findViewById(R.id.textview_v);

        this.button_upload = root.findViewById(R.id.button_upload);
        this.button_clear = root.findViewById(R.id.button_clear);
        this.button_sample = root.findViewById(R.id.button_sample);

        // Set listeners
        this.button_upload.setOnClickListener(this);
        this.button_clear.setOnClickListener(this);
        this.button_sample.setOnClickListener(this);

        // Refresh UI
        refresh();

        return root;
    }


    @Override
    public void onSampleLabel(Sample sample, Classification classification) {
        DataManager m = DataManager.getInstance();
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
        refresh();
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
}
