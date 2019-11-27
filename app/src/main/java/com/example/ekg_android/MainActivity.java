package com.example.ekg_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DeviceBluetoothInterface, View.OnClickListener, SettingDialog.SettingDialogListener, TrainFragmentInterface, MonitorFragmentInterface {


    // Bluetooth Manager
    private DeviceBluetoothManager bluetoothManager;

    // Connected flag
    private boolean isDeviceLocated = false;
    private boolean isConnected = false;

    // Connection state textview
    private TextView textView_connection_state;

    // Settings/Configuration button
    private Button button_settings;

    // Fragments
    private TrainFragment trainFragment;
    private MonitorFragment monitorFragment;


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
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MonitorFragment(this)).commit();

        // Add the connection state textview
        this.textView_connection_state = findViewById(R.id.textview_connection_state);

        // Add the settings button (not enabled until connected)
        this.button_settings = findViewById(R.id.button_settings);
        this.button_settings.setEnabled(false);

        // Set the settings button icon and callback
        this.button_settings.setBackgroundResource(R.drawable.ic_settings_applications_black_24dp);
        this.button_settings.setOnClickListener(this);

        // Setup a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(DataManager.channel_id, name, importance);
            channel.setDescription(description);

            // Registering channel
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            Fragment selectedFragment = null;
            switch (menuItem.getItemId()) {
                case R.id.nav_monitor: {
                    if (MainActivity.this.monitorFragment == null) {
                        MainActivity.this.monitorFragment = new MonitorFragment(MainActivity.this);
                    }
                    selectedFragment = MainActivity.this.monitorFragment;
                }
                break;

                case R.id.nav_train: {
                    if (MainActivity.this.trainFragment == null) {
                        MainActivity.this.trainFragment = new TrainFragment(MainActivity.this);
                    }
                    selectedFragment = MainActivity.this.trainFragment;
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
                        Log.i("BLE", "New Sample (TYPE = " + msg.get_sample_label() + ")");
                        DataManager.getInstance().addSample(new Sample(msg.get_sample_label(), msg.get_sample_amplitude(), msg.get_sample_period()));
                    }
                } catch (MessageSerializationException exception) {
                    Log.e("BLE", "Malformed message received!");
                }

            }
        });
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

    // Displays a notification
    public void displayNotificationForSample (Sample sample) {
        DataManager m = DataManager.getInstance();

        // Generate a notification (without intent)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DataManager.channel_id)
                .setSmallIcon(R.drawable.ic_nav_monitor)
                .setContentTitle("Heart Arrhythmia!")
                .setContentText("Detected a " + sample.getLabel().toString() + " signature pattern!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);


        // Get the notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // getNotificationID() returns a unique notification ID in compliance with usage
        notificationManager.notify(m.getNotificationID(), builder.build());
    }

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
                labelDialog.show(getSupportFragmentManager(), "Peak Detection");
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

    @Override
    public void onUploadTrainingData() {
        DataManager m = DataManager.getInstance();
        System.out.println("onUploadTrainingData()!");

        // Extract the Training Data Arrays
        ArrayList<Sample> ns = m.getNormalTrainingData();
        ArrayList<Sample> as = m.getAtrialTrainingData();
        ArrayList<Sample> vs = m.getVentricalTrainingData();

        // Validate the Training Data Arrays
        if (ns.size() != 20 || as.size() != 10 || vs.size() != 10) {
            Log.e("MainActivity", "Improperly sized training data arrays!");
            return;
        }

        // Create the training data message
        final Msg msg = new Msg();
        msg.configureAsTrainingMessage(ns, as, vs);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Attempt to serialize the message
                try {
                    Msg.MsgData data = msg.getSerialized();

                    // Enqueue the message for automatic dispatch
                    bluetoothManager.enqueueMessageBuffer(data.data);

                } catch (MessageSerializationException exception) {
                    exception.printStackTrace();
                }

                // Reconfigure the message to update configuration
                msg.configureAsInstructionDataMessage(MsgInstructionType.INST_EKG_CONFIGURE);


                // Attempt to serialize the message
                try {
                    Msg.MsgData data = msg.getSerialized();

                    // Enqueue the message for automatic disaptch
                    bluetoothManager.enqueueMessageBuffer(data.data);

                } catch (MessageSerializationException exception) {
                    exception.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onDisplayNotification(Sample sample) {
        this.displayNotificationForSample(sample);
    }
}
