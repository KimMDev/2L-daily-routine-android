package com.example.forcapstone2;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class BluetoothService extends Service {

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler();
    private boolean isScanning = false;
    private static final long SCAN_PERIOD = 10000;
    private static final String TARGET_MAC_ADDRESS = "1E:41:CF:5C:6F:4E"; // Your Arduino Nano BLE MAC address

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanLeDevice(true);
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(() -> {
                isScanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                Log.d("BluetoothService", "Stopped scanning");
            }, SCAN_PERIOD);

            isScanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
            Log.d("BluetoothService", "Started scanning");
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            Log.d("BluetoothService", "Stopped scanning");
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d("BluetoothService", "Found device: " + device.getAddress());
            if (TARGET_MAC_ADDRESS.equals(device.getAddress())) {
                Log.d("BluetoothService", "Target device found");
                connectToDevice(device);
                scanLeDevice(false);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        Log.d("BluetoothService", "Connecting to device: " + device.getAddress());
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("BluetoothService", "Connected to GATT server.");
                bluetoothGatt.discoverServices();
                showToast("Connected to " + gatt.getDevice().getName());
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i("BluetoothService", "Disconnected from GATT server.");
                showToast("Disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothService", "Services discovered.");
            } else {
                Log.w("BluetoothService", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Handle characteristic read
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Handle characteristic changed
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    private void showToast(final String message) {
        handler.post(() -> Toast.makeText(BluetoothService.this, message, Toast.LENGTH_SHORT).show());
    }
}
