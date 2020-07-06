package com.example.esenseapplication.services;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.example.esenseapplication.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseEvent;
import io.esense.esenselib.ESenseEventListener;
import io.esense.esenselib.ESenseManager;
import io.esense.esenselib.ESenseSensorListener;

public class IMUService {
    String TAG = "IMUService";
    Context context;
    // Handler object to update the main screen
    Handler mainHandler;

    // eSense device name, starting with "eSense-"
    String deviceName;
    // file name to save data
    String fileId;
    // ESenseManager object to manage the BLE connection
    ESenseManager eSenseManager;
    // configuration of the IMU
    ESenseConfig eSenseConfig;

    // saved IMU data
    ArrayList<ESenseEvent> eventList;

    // IMU state
    public enum IMUState {
        STOPPED, COLLECTING, WRITING, ERROR;

        public String toString(){
            switch (this){
                case STOPPED:
                    return "stopped";
                case COLLECTING:
                    return "collecting data";
                case WRITING:
                    return "writing data";
                case ERROR:
                    return "error";
            }
            return null;
        }
    }
    IMUService.IMUState imuSate = IMUService.IMUState.STOPPED;

    public IMUService(Context context, Handler mainHandler, String deviceName, ESenseManager eSenseManager){
        this.context = context;
        this.mainHandler = mainHandler;
        this.deviceName = deviceName;
        this.eSenseManager = eSenseManager;

        // get the ESenseConfig object
        eSenseManager.registerEventListener(eSenseEventListener);
        try {
//            while (!eSenseManager.getSensorConfig()) {
//                Log.i(TAG, "reading ESenseConfig failed, trying again");
//                Thread.sleep(1000);
//            }
            // set the default config
            ESenseConfig defaultConfig = new ESenseConfig(
                    ESenseConfig.AccRange.G_4,
                    ESenseConfig.GyroRange.DEG_1000,
                    ESenseConfig.AccLPF.DISABLED,
                    ESenseConfig.GyroLPF.DISABLED);
            while (!eSenseManager.setSensorConfig(defaultConfig)) {
                Log.i(TAG, "setting ESenseConfig failed, trying again");
                Thread.sleep(1000);
            }

            // read the battery voltage
            while (!eSenseManager.getBatteryVoltage()) {
                Log.i(TAG, "getting battery voltage failed, trying again");
                Thread.sleep(1000);
            }

            // read the default interval values
            while (!eSenseManager.getAdvertisementAndConnectionInterval()) {
                Log.i(TAG, "reading intervals failed, trying again");
                Thread.sleep(1000);
            }

            // set the default interval values
            int minConn = 7, maxConn = 27;
            while (!eSenseManager.setAdvertisementAndConnectiontInterval(100, 150, minConn, maxConn)) {
                Log.i(TAG, "setting intervals failed, trying again");
                Thread.sleep(1000);
            }

            // update the main UI
            Util.SendMessage(mainHandler, 1, defaultConfig);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        fileId = null;
    }

    public ESenseConfig getESenseConfig(){
        return eSenseConfig;
    }

    public void setESenseConfig(ESenseConfig eSenseConfig){
        this.eSenseConfig = eSenseConfig;
    }

    private void setState(IMUService.IMUState imuSate) {
        this.imuSate = imuSate;
        // update the main screen
        Util.SendMessage(mainHandler, 2, imuSate.toString());
    }

    // start IMU data collection
    public void startIMU(String fileId){
        switch (imuSate){
            case STOPPED:
            case ERROR:
                // initialize the variable to save data
                eventList = new ArrayList<ESenseEvent>();
                // set the file name to save data
                this.fileId = fileId;
                // start collecting data
                int samplingRate = 100;
                eSenseManager.registerSensorListener(eSenseSensorListener, samplingRate);
                // set state
                setState(IMUService.IMUState.COLLECTING);
        }
    }

    // stop IMU data collection
    public void stopIMU(){
        switch (imuSate){
            case COLLECTING:
                // stop recording
                eSenseManager.unregisterSensorListener();
                // set state
                setState(IMUService.IMUState.WRITING);
                // write the data
                writeEventToFile(fileId);
            case ERROR:
                // release the data
                eventList = null;
                // set state
                setState(IMUService.IMUState.STOPPED);
                // set fileName to null
                fileId = null;
        }
    }

    // ESenseSensorListener object to handle IMU data
    ESenseSensorListener eSenseSensorListener = new ESenseSensorListener() {
        @Override
        public void onSensorChanged(ESenseEvent eSenseEvent) {
            Log.i(TAG, "" + (eSenseConfig == null));
            double[] acc = eSenseEvent.convertAccToG(eSenseConfig);
            double[] gyro = eSenseEvent.convertGyroToDegPerSecond(eSenseConfig);
            Log.i(TAG, "acc: " + acc[0] + "," + acc[1] + "," + acc[2]);
            Log.i(TAG, "gyro: " + gyro[0] + "," + gyro[1] + "," + gyro[2]);

            // save the event
            eventList.add(eSenseEvent);
        }
    };

    // ESenseEventListener object to handle eSense events
    ESenseEventListener eSenseEventListener = new ESenseEventListener() {
        @Override
        public void onBatteryRead(double v) {
            Log.i(TAG, "onBatteryRead");

            String str = String.format("battery voltage = %f", v);
            Log.i(TAG, str);
        }

        @Override
        public void onButtonEventChanged(boolean b) {
            Log.i(TAG, "onButtonEventChanged");
        }

        @Override
        public void onAdvertisementAndConnectionIntervalRead(int i, int i1, int i2, int i3) {
            Log.i(TAG, "onAdvertisementAndConnectionIntervalRead");

            String str = String.format("minAdv = %d, maxAdv = %d, minConn = %d, maxConn = %d", i, i1, i2, i3);
            Log.i(TAG, str);
        }

        @Override
        public void onDeviceNameRead(String s) {
            Log.i(TAG, "onDeviceNameRead");
        }

        @Override
        public void onSensorConfigRead(ESenseConfig eSenseConfig) {
            Log.i(TAG, "onSensorConfigRead");

            Util.SendMessage(mainHandler, 1, eSenseConfig);
        }

        @Override
        public void onAccelerometerOffsetRead(int i, int i1, int i2) {
            Log.i(TAG, "onAccelerometerOffsetRead");
        }
    };

    private void writeEventToFile(String fileId){
        Log.i(TAG, "writeEventToFile");

        if (eventList == null){
            Log.i(TAG,"eventList is null");
            return;
        }

        // get the directory to save data
        String dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        // write to a CSV file
        String fileName = fileId + ".csv";
        File csvFile = new File(dir, fileName);
        Log.i(TAG, "IMU data will be saved to: " + csvFile.getAbsolutePath());
        try {
            FileWriter fw = new FileWriter(csvFile);
            BufferedWriter bw = new BufferedWriter(fw);
            // header
            String header = "timestamp, timestamp_formatted, acc0, acc1, acc2, gyro0, gyro1, gyro2\n";
            bw.write(header);
            bw.flush();
            // content
            for (ESenseEvent event : eventList){
                double[] acc = event.convertAccToG(eSenseConfig);
                double[] gyro = event.convertGyroToDegPerSecond(eSenseConfig);

                long timestamp = event.getTimestamp();
                Date date = new Date(timestamp);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String timestampFormatted = simpleDateFormat.format(date);

                Log.i(TAG, date.toString());
                String line = String.format("%d, %s, %f,%f,%f,%f,%f,%f\n",
                        timestamp, timestampFormatted, acc[0], acc[1], acc[2], gyro[0], gyro[1], gyro[2]);
                bw.write(line);
                bw.flush();
            }
            // close the file
            bw.close();
            fw.close();
        }
        catch (Exception e){
            e.printStackTrace();
            setState(IMUService.IMUState.ERROR);
        }
    }

    public void readIntervals(){
        this.eSenseManager.getAdvertisementAndConnectionInterval();
    }
}

