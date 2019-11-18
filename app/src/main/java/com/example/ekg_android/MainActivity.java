package com.example.ekg_android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements DeviceBluetoothInterface {


    // Bluetooth Manager
    private DeviceBluetoothManager bluetoothManager;

    // Connected flag
    private boolean isDeviceLocated = false;
    private boolean isConnected = false;

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
    public void onBluetoothConnect(boolean didSucceed) {

        // Simply display a notification if thing succeed or not
        if (didSucceed) {
            this.isConnected = this.isDeviceLocated = true;
            displaySuccessSnackbar("Connected");
        } else {
            this.isConnected = this.isDeviceLocated = false;
            displayFailureSnackbar("Unable to connect to device!");
        }
    }

    @Override
    public void onCharacteristicChanged(byte[] value) {
        System.out.println("Data received (" + value.length + " bytes)");

        // Parse message

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
}
