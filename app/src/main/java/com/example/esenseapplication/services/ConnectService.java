package com.example.esenseapplication.services;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseConnectionListener;
import io.esense.esenselib.ESenseManager;

public class ConnectService {
    String TAG = "ConnectService";
    Context context;
    Handler mainHandler;

    // eSense device name, starting with "eSense-"
    String deviceName;
    // ESenseManager object to manage the BLE connection
    ESenseManager eSenseManager;
    // configuration of the IMU
    ESenseConfig eSenseConfig;

    public ConnectService(Context context, Handler mainHandler, String deviceName){
        Log.i(TAG, "ConnectService");
        this.context = context;
        this.mainHandler = mainHandler;
        this.deviceName = deviceName;

        eSenseManager = new ESenseManager(deviceName, context, eSenseConnectionListener);
        eSenseConfig = new ESenseConfig(ESenseConfig.AccRange.G_4, ESenseConfig.GyroRange.DEG_1000, ESenseConfig.AccLPF.BW_5, ESenseConfig.GyroLPF.BW_5);
    }

    // ESenseConnectionListener to handle the BLE connection
    private ESenseConnectionListener eSenseConnectionListener = new ESenseConnectionListener() {
        @Override
        public void onDeviceFound(ESenseManager eSenseManager) {
            Log.i(TAG, "onDeviceFound");
        }

        @Override
        public void onDeviceNotFound(ESenseManager eSenseManager) {
            Log.i(TAG, "onDeviceNotFound");

            // update UI
            sendMessage(1, "device not found, try again");
        }

        @Override
        public void onConnected(ESenseManager eSenseManager) {
            Log.i(TAG, "onConnected");

            // send the eSense configuration every time when connected
            eSenseManager.setSensorConfig(eSenseConfig);
            // update UI
            sendMessage(2, eSenseManager);
        }

        @Override
        public void onDisconnected(ESenseManager eSenseManager) {
            Log.i(TAG, "onDisconnected");

            // update UI
            sendMessage(1, "disconnected");
        }
    };

    // connect to eSense
    public void connect(){
        Log.i(TAG, "connect");

        // update UI
        sendMessage(1, "connecting");
        // start connecting to eSense
        int TIMEOUT = 1000;
        eSenseManager.connect(TIMEOUT);

    }

    public void sendMessage(int what, Object obj){
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        mainHandler.sendMessage(message);
    }
}
