package com.example.ekg_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.Semaphore;

public class DeviceBluetoothManager {

    // UUID of the single device service in the broadcasted profile
    private UUID uuid_device_service = intToUUID(0x00FF);

    // UUID of the multipurpose characteristic of the device service
    private UUID uuid_device_characteristic = intToUUID(0xFF01);

    // UUID of the descriptor for the characteristic
    private UUID uuid_device_characteristic_descriptor = intToUUID(0x2902);

    // Bluetooth Adapter
    private BluetoothAdapter bluetoothAdapter;

    // Bluetooth Manager
    private BluetoothManager bluetoothManager;

    // Bluetooth device
    private BluetoothDevice bluetoothDevice;

    // GATT interface object
    private BluetoothGatt gatt;

    // Service interface object
    private BluetoothGattService service;

    // Characteristic interface object
    private BluetoothGattCharacteristic characteristic;

    // State Variable: True if the Bluetooth Manager and Adapter are ready
    private boolean isBluetoothReady = false;

    // State Variable: True if the Bluetooth device with a given identifier has been located
    private boolean isDeviceLocated = false;

    // State Variable: True if the application is connected to the device
    private boolean isDeviceConnected = false;

    // Delegate for receiving callbacks from this class
    private DeviceBluetoothInterface delegate;

    // Semaphore for write access (init with value 1)
    private Semaphore writeSemaphore;

    // Name of device to scan for
    private String device_name = null;

    // Tracks the number of scan results checked so far
    private int scan_results_checked = 0;

    // The maximum number of scan results to check before abandoning search
    private static int max_scan_results_to_check = 50;


    /*
     *******************************************************************************
     *                 Asynchronous Task for Characteristic Writes                 *
     *******************************************************************************
    */


    // Runnable background thread
    private class WriteDispatchTask extends AsyncTask<byte[], byte[], byte[]> {

        // This is run in a background thread
        @Override
        protected byte[] doInBackground(byte[]... params) {

            // Get the data from params
            byte[] data = params[0];

            System.out.println("Task: Acquiring write-semaphore!");

            // Wait on the write-semaphore
            try {
                DeviceBluetoothManager.this.writeSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return data;
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(byte[] data) {
            super.onPostExecute(data);

            // Write data to the characteristic
            DeviceBluetoothManager.this.characteristic.setValue(data);

            System.out.println("Task: Got lock and issued write!");

            // Write characteristic
            boolean outcome = gatt.writeCharacteristic(characteristic);

            // Notify delegate
            if (delegate != null) {
                delegate.onCharacteristicWrite(outcome);
            }
        }
    }


    /*
     *******************************************************************************
     *                       Bluetooth Callbacks & Handlers                        *
     *******************************************************************************
    */


    /* @brief: Callback for Bluetooth GAP events */
    private ScanCallback getScanCallback () {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String result_name = result.getDevice().getName();
                String device_name = DeviceBluetoothManager.this.device_name;

                // If we are out of scan results to check, stop and conclude
                if (scan_results_checked == max_scan_results_to_check) {
                    scan_results_checked = 0;

                    // Stop scanning
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(this);

                    // Mark device not located
                    DeviceBluetoothManager.this.isDeviceLocated = false;

                    // Inform delegate if set
                    if (DeviceBluetoothManager.this.delegate != null) {
                        delegate.onDeviceLocated(false);
                    }
                } else {

                    // Increment scan count
                    scan_results_checked++;

                    // Ignore device if name is null
                    if (result_name == null) {
                        return;
                    }

                    // Ignore device if name does not match
                    if (device_name.compareTo(result_name) != 0) {
                        return;
                    }

                    // Otherwise a match: Update device
                    DeviceBluetoothManager.this.bluetoothDevice = result.getDevice();

                    // Stop scanning
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(this);

                    // Mark device located
                    DeviceBluetoothManager.this.isDeviceLocated = true;

                    // Invoke delegate if set
                    if (DeviceBluetoothManager.this.delegate != null) {
                        delegate.onDeviceLocated(true);
                    }
                }

            }
        };
    }

    /* @brief: Callback for Bluetooth GATT events */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else {
                DeviceBluetoothManager.this.onBluetoothDisconnect();
            }
        }

        @Override
        public void onServicesDiscovered (BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            boolean didConnect = (status == BluetoothGatt.GATT_SUCCESS);

            if (didConnect == false) {
                DeviceBluetoothManager.this.onBluetoothDisconnect();
            } else {
                DeviceBluetoothManager.this.isDeviceConnected = true;

                // Update Service and Characteristic objects
                service = gatt.getService(uuid_device_service);
                characteristic = service.getCharacteristic(uuid_device_characteristic);

                // Set whether characteristic is enabled
                if (gatt.setCharacteristicNotification(characteristic, true) == false) {
                    Log.e("BLE", "Characteristic could not be enabled!?");
                    return;
                }

                // Set the write-type for the characteristic
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                // Log descriptors (if any)
                if (characteristic.getDescriptors().size() > 0) {
                    for (BluetoothGattDescriptor d : characteristic.getDescriptors()) {
                        Log.i("BLE", "BluetoothGattDescriptor: " + d.getUuid().toString());
                    }
                } else {
                    Log.e("BLE", "No BluetoothGattDescriptors");
                }

                // Now setup the descriptor to enable notifications (aperiodic feedback from device)
                BluetoothGattDescriptor descriptor =
                        characteristic.getDescriptor(uuid_device_characteristic_descriptor);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

                // Inform delegate (on disconnect will also inform delegate)
                if (DeviceBluetoothManager.this.delegate != null) {
                    DeviceBluetoothManager.this.delegate.onBluetoothConnect(didConnect);
                }
            }
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic
                                             characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            // Extract response data
            byte[] characteristic_value_data = characteristic.getValue();

            // Log for debug reasons
            Log.i("BLE", "Received characteristic data:");
            log_hex_buffer(characteristic_value_data);

            // If a delegate is set, then call it with the data
            if (delegate != null) {
                delegate.onCharacteristicChanged(characteristic_value_data);
            }
        }

        @Override
        public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic
                                          characteristic, int status) {
            Log.i("BLE", "Characteristic Read Event");
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic
                                           characteristic, int status) {

            // Release the write semaphore
            System.out.println("Releasing write-semaphore!");
            DeviceBluetoothManager.this.writeSemaphore.release();

            // Log error if didn't succeed
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLE", "Last write to device characteristic did not succeed");
            } else {
                Log.i("BLE", "Characteristic write successful!");
            }

            // Update delegate on outcome of write operation
            if (delegate != null) {
                delegate.onCharacteristicWrite((status == BluetoothGatt.GATT_SUCCESS));
            }
        }
    };


    /* @brief Handles disconnection. Cleans up internal variables; Notifies delegate if set */
    private void onBluetoothDisconnect () {
        gatt.disconnect();
        this.isDeviceConnected = false;
        this.service = null;
        this.characteristic = null;
        if (delegate != null) {
            delegate.onBluetoothConnect(false);
        }
    }

    /*
     *******************************************************************************
     *                              Interface Methods                              *
     *******************************************************************************
    */


    /* @brief: Enqueues data to be written to the characteristic
     * @note: Asynchronous send since a thread must wait until characteristic is available
     * @param data: The buffer of bytes to write to the characteristic
     */
    public void enqueueMessageBuffer (byte[] data) {

        // Log data being sent
        System.out.println("enqueueMessageBuffer()");
        log_hex_buffer(data);

        // Launch a thread that will wait until it can send the next item
        new WriteDispatchTask().execute(data);
    }

    /* @brief: Returns whether Bluetooth is ready or not
     * @return boolean indicating whether the Bluetooth adapter is ready and/or enabled
     */
    public boolean isBluetoothReady () {
        return (this.bluetoothAdapter != null && this.bluetoothAdapter.isEnabled());
    }

    /* @brief: Starts a Bluetooth scan for a device carrying the given name
     * @param: Name/Identifier of the BLE device to locate
     * @raise: Exception if trying to scan when already connected
     * callback: When outcome is known, it calls onDeviceLocated method of the delegate
     */
    public void scanForDevice (String identifier) throws DeviceBluetoothException {
        if (this.isDeviceConnected) {
            throw new DeviceBluetoothException("Cannot scan if already connected");
        }
        this.scan_results_checked = 0;
        this.device_name = identifier;
        this.bluetoothAdapter.getBluetoothLeScanner().startScan(getScanCallback());
    }

    /* @brief: Attempts to connect to a Bluetooth LE device
     * @raise: Exception if no device is set
     * @callback: When an outcome is known, it calls method onDidConnect of the delegate
     */
    public void connectDevice (Context c) throws DeviceBluetoothException {
        if (this.bluetoothDevice == null) {
            throw new DeviceBluetoothException("No device instance to connect to");
        }
        DeviceBluetoothManager.this.gatt = this.bluetoothDevice.connectGatt(c, true,
                DeviceBluetoothManager.this.gattCallback);
    }

    /* @brief: Disconnects from the Bluetooth LE device
     * @callback: When disconnected, it calls method of onDidConnect of the delegate
     */
    public void disconnectDevice () {
        if (this.bluetoothDevice == null || this.isDeviceConnected == false) {
            return;
        }
        this.onBluetoothDisconnect();
    }


    /*
     *******************************************************************************
     *                           Internal Helper Methods                           *
     *******************************************************************************
    */


    /* @brief: Converts an integer to a UUID which can be compared to those broadcast by BLE devices
     * @param i: Integer to convert
     */
    public static UUID intToUUID (int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    /* @brief: Logs a hex buffer to standard output */
    public static void log_hex_buffer (byte[] data) {
        System.out.printf("Data[%d] = {", data.length);
        for (int i = 0; i < data.length; ++i) {
            System.out.printf("%X ", data[i]);
        }
        System.out.println("}");
    }



    /* @brief: Constructor for the device bluetooth manager */
    public DeviceBluetoothManager (Context context, DeviceBluetoothInterface delegate) {

        // Assign the delegate
        this.delegate = delegate;

        // Initialize Bluetooth Manager
        this.bluetoothManager =
                (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);

        // Set the adapter
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        // Update state
        this.isBluetoothReady = (bluetoothAdapter != null && bluetoothAdapter.isEnabled());

        // Set the write semaphore
        writeSemaphore = new Semaphore(1, true);

        Log.i("BLE", "Initialized ");
    }

}
