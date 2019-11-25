package com.example.ekg_android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class SettingDialog extends AppCompatDialogFragment implements AdapterView.OnItemSelectedListener {

    // The configuration fields
    private Comparator cfg_comp;
    private int cfg_val;

    // The spinner for selecting the configuration
    private Spinner spinner_comparator;

    // The EditText for capturing the threshold value
    private EditText edittext_threshold;

    // The listener for callback events
    private SettingDialogListener listener;

    // Constructor to save the sample
    public SettingDialog(Comparator comparator, int cfg_val, SettingDialogListener listener) {
        this.cfg_comp = comparator;
        this.cfg_val  = cfg_val;
        this.listener = listener;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.label_dialog, null);

        // Set spinner and textviews
        this.spinner_comparator = view.findViewById(R.id.spinner_comparator);
        this.edittext_threshold = view.findViewById(R.id.edittext_threshold);


        // Set the array adapter and spinner selection
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.comparators, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_comparator.setAdapter(adapter);
        spinner_comparator.setOnItemSelectedListener(this);
        spinner_comparator.setSelection(comparatorToIndex(this.cfg_comp));


        // Set the edittext threshold
        this.edittext_threshold.setText(String.format("%d", this.cfg_val));


        builder.setView(view);
        builder.setTitle("Settings");
        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int threshold = 0;
                try {
                    threshold = Integer.parseInt(SettingDialog.this.edittext_threshold.getText().toString());
                }
                catch (NumberFormatException e)
                {
                    Log.e("SettingDialog", "Problem converting string to integer!");
                    threshold = 0;
                    e.printStackTrace();
                }

                SettingDialog.this.listener.onSettingsChanged(SettingDialog.this.cfg_comp,
                        threshold);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                System.out.println("Closed the settings dialog...");
            }
        });

        return builder.create();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (i) {
            case 0:
                this.cfg_comp = Comparator.GREATER_THAN;
                break;
            case 1:
                this.cfg_comp = Comparator.LESS_THAN;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // Returns an index that corresponds to the position in the comparators array of the given type
    private static int comparatorToIndex (Comparator c) {
        switch (c) {
            case GREATER_THAN: return 0;
            case LESS_THAN:    return 1;
        }
        return 0;
    }


    public interface SettingDialogListener {
        void onSettingsChanged (Comparator comparator, int threshold);
    }
}
