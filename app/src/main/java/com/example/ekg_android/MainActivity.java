package com.example.ekg_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements DeviceBluetoothInterface, View.OnClickListener, SettingDialog.SettingDialogListener {


    // Bluetooth Manager
    private DeviceBluetoothManager bluetoothManager;

    // Connected flag
    private boolean isDeviceLocated = false;
    private boolean isConnected = false;

    // Connection state textview
    private TextView textView_connection_state;

    // Settings/Configuration button
    private Button button_settings;


    /*
     *******************************************************************************
     *                               UI Interactions                               *
     *******************************************************************************
    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Initialize the bluetooth manager
        this.bluetoothManager = new DeviceBluetoothManager(getApplicationContext(), this);

        // Add bottom navigation, and configure the listener for changes
        BottomNavigationView bottom_navigation = findViewById(R.id.bottom_navigation);
        bottom_navigation.setOnNavigationItemSelectedListener(navigationListener);

        bottom_navigation.setSelectedItemId(R.id.nav_monitor);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MonitorFragment()).commit();

        // Add the connection state textview
        this.textView_connection_state = findViewById(R.id.textview_connection_state);

        // Add the settings button (not enabled until connected)
        this.button_settings = findViewById(R.id.button_settings);
        this.button_settings.setEnabled(false);

        // Set the settings button icon and callback
        this.button_settings.setBackgroundResource(R.drawable.ic_settings_applications_black_24dp);
        this.button_settings.setOnClickListener(this);

    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            Fragment selectedFragment = null;
            switch (menuItem.getItemId()) {
                case R.id.nav_monitor: {
                    selectedFragment = new MonitorFragment();
                }
                break;

                case R.id.nav_train: {
                    selectedFragment = new TrainFragment();
                }
                break;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();

            return true;
        }
    };


    @Override
    protected void onPostResume() {
        super.onPostResume();

        // If bluetooth isn't initialized, then initialize it
        if (this.bluetoothManager.isBluetoothReady() == false) {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize Bluetooth
        if (this.isDeviceLocated == false) {
            this.displayActionSnackbar("Scanning for device ...");
            try {
                bluetoothManager.scanForDevice("EKG-ESP32");
            } catch (DeviceBluetoothException exception) {
                System.err.println(exception.toString());
                exception.printStackTrace();
            }
        }
    }


    /*
     *******************************************************************************
     *                                  Messages                                   *
     *******************************************************************************
    */

    // Sends an instruction
    private void send_instruction (MsgInstructionType instructionType) {
        Msg message = new Msg().configureAsInstructionDataMessage(instructionType);
        try {
            Msg.MsgData data = message.getSerialized();
            if (isConnected) {
                bluetoothManager.enqueueMessageBuffer(data.data);
            }
        } catch (MessageSerializationException exception) {
            exception.printStackTrace();
        }
    }

    /*
     *******************************************************************************
     *                                BLE Callbacks                                *
     *******************************************************************************
    */


    @Override
    public void onDeviceLocated(boolean didSucceed) {

        // If the device was successfully found, then we connect to it
        if (didSucceed) {
            this.displaySuccessSnackbar("Device located!");
            this.isDeviceLocated = true;
            try {
                bluetoothManager.connectDevice(getApplicationContext());
            } catch (DeviceBluetoothException exception) {
                System.err.println(exception.toString());
                exception.printStackTrace();
            }
        } else {
            this.isDeviceLocated = false;
            System.err.println("Unable to locate the device!");
            displayFailureSnackbar("Unable to locate the device!");
        }
    }

    @Override
    public void onBluetoothConnect(final boolean didSucceed) {
        int connected = ContextCompat.getColor(getApplicationContext(), R.color.colorGreen);
        int disconnected = ContextCompat.getColor(getApplicationContext(), R.color.colorWhite);
        String text;
        int    color;

        // Simply display a notification if thing succeed or not
        if (didSucceed) {
            this.isConnected = this.isDeviceLocated = true;
            displaySuccessSnackbar("Connected");
            text = "Connected";
            color = connected;

        } else {
            this.isConnected = this.isDeviceLocated = false;
            text = "Disconnected";
            color = disconnected;
            displayFailureSnackbar("Unable to connect to device!");
        }

        // Update UI elements
        runOnUiThread(new StatusUIThread(color, text) {
            @Override
            public void run () {

                // Update the UI
                MainActivity.this.textView_connection_state.setText(this.text);
                MainActivity.this.textView_connection_state.setTextColor(this.color);
                MainActivity.this.button_settings.setEnabled(didSucceed);

                if (didSucceed) {
                    // Try to turn on relaying
                    send_instruction(MsgInstructionType.INST_EKG_START);
                }
            }
        });

    }

    @Override
    public void onCharacteristicChanged(final byte[] value) {
        System.out.println("Data received (" + value.length + " bytes)");

        // Parse message
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Msg msg = new Msg().fromByteBuffer(value);
                    if (msg.getMessageType() == MsgType.MSG_TYPE_SAMPLE_DATA) {
                        System.out.printf("Sample: Label = %s\tAmplitude = %d\tRR-Period = %d\n", msg.get_sample_label().toString(), msg.get_sample_amplitude(), msg.get_sample_period());
                        DataManager.getInstance().addSample(new Sample(msg.get_sample_label(), msg.get_sample_amplitude(), msg.get_sample_period()));
                    }
                } catch (MessageSerializationException exception) {
                    Log.e("BLE", "Malformed message received!");
                }

            }
        });
        // If sample and training not finished -> then push to sample queue
        // If training not finished, it should be in sampling mode

        // If training finished, it should be in monitor mode


        // If sample and training finished -> push to monitor queue
    }

    @Override
    public void onCharacteristicWrite(boolean didSucceed) {
        if (didSucceed) {
            displaySuccessSnackbar("Changes sent successfully");
        } else {
            displayFailureSnackbar("Changes did not send successfully!");
        }
    }


    /*
     *******************************************************************************
     *                                    Misc                                     *
     *******************************************************************************
    */


    // Display a temporary message acknowledging an action
    public void displayActionSnackbar (String msg) {
        Snackbar s = Snackbar.make(this.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT);
        View bg = s.getView();
        bg.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorYellow));
        TextView fg = bg.findViewById(R.id.snackbar_text);
        fg.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));
        s.show();
    }

    // Displays a temporary message displaying action success
    public void displaySuccessSnackbar (String msg) {
        Snackbar s = Snackbar.make(this.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT);
        View bg = s.getView();
        bg.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGreen));
        TextView fg = bg.findViewById(R.id.snackbar_text);
        fg.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));
        s.show();
    }

    // Displays a temporary message displaying action failure
    public void displayFailureSnackbar (String msg) {
        Snackbar s = Snackbar.make(this.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT);
        View bg = s.getView();
        bg.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWarning));
        TextView fg = bg.findViewById(R.id.snackbar_text);
        fg.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));
        s.show();
    }

    @Override
    public void onClick(View view) {
        DataManager m = DataManager.getInstance();
        switch (view.getId()) {
            case R.id.button_settings: {
                Comparator comparator = m.getSettingComparator();
                int threshold = m.getSettingThreshold();
                SettingDialog labelDialog = new SettingDialog(comparator, threshold, this);
                labelDialog.show(getSupportFragmentManager(), "Settings");
            }
            break;
        }
    }

    @Override
    public void onSettingsChanged(Comparator comparator, int threshold) {

        System.out.printf("Settings Updated (comp = %s, threshold = %d)\n", comparator.toString(), threshold);

        // Update the configuration settings
        DataManager.getInstance().setSettings(comparator, threshold);

        // Transmit changes to the device (if connected - although only accessible if connected)
        System.out.println("Transmitting changes to device ...");

        if (this.isConnected) {

            // Create a new message and set it as a Configuration message
            Msg msg = new Msg();
            msg.configureAsConfigurationMessage(comparator, threshold);

            // Try to serialize and send it
            try {
                Msg.MsgData data = msg.getSerialized();
                bluetoothManager.enqueueMessageBuffer(data.data);
            } catch (MessageSerializationException exception) {
                exception.printStackTrace();
            }

            // Create a new message and set it as an Instruction message
            msg = new Msg();
            msg.configureAsInstructionDataMessage(MsgInstructionType.INST_EKG_CONFIGURE);

            // Try to serialize and send it
            try {
                Msg.MsgData data = msg.getSerialized();
                bluetoothManager.enqueueMessageBuffer(data.data);
            } catch (MessageSerializationException exception) {
                exception.printStackTrace();
            }
        }

    }
}
