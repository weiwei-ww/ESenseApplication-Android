package com.example.esenseapplication.services;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ScanService {
    String TAG = "ScanService";
    Context context;
    // Handler object to update UI
    Handler mainHandler;

    // eSense device name
    String deviceName;

    // scanning state
    private enum ScanState{
        STOPPED, SCANNING;
    }

    ScanState scanState;

    // objects for scanning BLE devices
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;

    public ScanService(Context context, Handler mainHandler){
        Log.i(TAG, "ScanService");
        this.context = context;
        this.mainHandler = mainHandler;

        deviceName = null;
        scanState = ScanState.STOPPED;

        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // ask the user to enable Bluetooth, if Bluetooth is not enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Activity activity = (Activity) context;
            activity.startActivityForResult(enableBtIntent, 1);
        }
        // get the scanner object
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public ScanState getScanState() {
        return scanState;
    }

    public void setScanState(ScanState scanState) {
        this.scanState = scanState;
    }

    // the callback function to deal with scanning results
    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            super.onScanResult(callbackType, result);

            // get the name of the device
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            // ignore devices other than eSense
            if (deviceName == null || !deviceName.startsWith("eSense-"))
                return;
            Log.i(TAG, "device found (" + deviceName + ")");
            setDeviceName(deviceName);

            // update UI
            sendMessage(2, deviceName);

            // stop scanning once found eSense
            Log.i(TAG, "eSense found, stop scanning");
            bluetoothLeScanner.stopScan(scanCallback);
        }
    };

    // start BLE scanning
    public void startScan(){
        Log.i(TAG, "startScan");

        switch (getScanState()){
            case STOPPED:
                // start scanning
                bluetoothLeScanner.startScan(scanCallback);
                // set state
                setScanState(ScanState.SCANNING);
                // update UI
                sendMessage(1, "scanning");
                break;
            case SCANNING:
                Log.i(TAG, "already scanning");
                break;
        }
    }

    // stop BLE scanning
    public void stopScan(){
        Log.i(TAG, "stopScan");

        switch (getScanState()){
            case STOPPED:
                Log.i(TAG, "already stopped scanning");
                break;
            case SCANNING:
                // start scanning
                bluetoothLeScanner.stopScan(scanCallback);
                // set state
                setScanState(ScanState.STOPPED);
                // update UI
                sendMessage(1, "no device found");
                break;
        }
    }

    public void sendMessage(int what, Object obj){
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        mainHandler.sendMessage(message);
    }
}
