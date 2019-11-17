package com.example.ekg_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TrainFragment extends Fragment {

    // Used to control colors
    private enum mode {
        MODE_TRAINING,
        MODE_FINISHED
    };

    // Metadata


    // Textviews
    private TextView textview_n_count;
    private TextView textview_a_count;
    private TextView textview_v_count;
    private TextView textview_period;
    private TextView textview_amplitude;

    // Buttons
    private Button button_upload;
    private Button button_clear;
    private Button button_sample;
    private Button button_classify_a;
    private Button button_classify_v;
    private Button button_classify_n;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_train, container, false);
    }


    // Updates the UI based on the mode
    private void updateUIForMode (mode m) {
        switch (m) {
            case MODE_TRAINING: {

            }
            break;
            case MODE_FINISHED: {

            }
            break;
        }
    }

    void setSampleC () {

    }
}
