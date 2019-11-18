package com.example.ekg_android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class LabelDialog extends AppCompatDialogFragment implements AdapterView.OnItemSelectedListener {

    // Fields
    private Sample sample = null;
    private Spinner spinner_label;
    private TextView textview_interval;
    private TextView textview_amplitude;
    private Classification classification = Classification.NONE;
    private LabelDialogListener listener;

    // Constructor to save the sample
    public LabelDialog (Sample sample, LabelDialogListener listener) {
        this.sample = sample;
        this.listener = listener;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.label_dialog, null);

        // Set spinner and textviews
        this.spinner_label = view.findViewById(R.id.spinner_label);
        this.textview_interval = view.findViewById(R.id.textview_interval);
        this.textview_amplitude = view.findViewById(R.id.textview_amplitude);

        // Set array adaptor
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_label.setAdapter(adapter);
        spinner_label.setOnItemSelectedListener(this);

        // Set text for sample
        this.textview_interval.setText(String.format("%d ms", sample.getPeriod()));
        this.textview_amplitude.setText(String.format("%d V", sample.getAmplitude()));


        builder.setView(view);
        builder.setTitle("Sample");
        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                LabelDialog.this.listener.onSampleLabel(LabelDialog.this.sample, LabelDialog.this.classification);
            }
        });

        return builder.create();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (i) {
            case 0:
                this.classification = Classification.NONE;
                break;
            case 1:
                this.classification = Classification.NORMAL;
                break;
            case 2:
                this.classification = Classification.ATRIAL;
                break;
            case 3:
                this.classification = Classification.VENTRICAL;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    public interface LabelDialogListener {
        void onSampleLabel (Sample sample, Classification classification);
    }
}
