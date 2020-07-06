package com.example.esenseapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.example.esenseapplication.services.AudioRecorderService;
import com.example.esenseapplication.services.IMUService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.esense.esenselib.ESenseConfig;
import io.esense.esenselib.ESenseManager;

public class DataCollectionActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "DataCollectionActivity";

    Button btn_start, btn_stop, btn_readIntervals, btn_changeAudioRecordingOn;
    TextView tv_deviceName, tv_imuStatus, tv_recordingStatus, tv_audioRecordingOn;
    EditText et_fileName;

    IMUService imuService;
    AudioRecorderService audioRecorderService;

    boolean audioRecordingOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        btn_start = DataCollectionActivity.this.findViewById(R.id.btn_start);
        btn_start.setOnClickListener(this);
        btn_stop = DataCollectionActivity.this.findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(this);
        btn_readIntervals = DataCollectionActivity.this.findViewById(R.id.btn_readIntervals);
        btn_readIntervals.setOnClickListener(this);
        btn_changeAudioRecordingOn = DataCollectionActivity.this.findViewById(R.id.btn_changeAudioRecordingOn);
        btn_changeAudioRecordingOn.setOnClickListener(this);
//      disable the buttons until the ESenseConfig object is received
        btn_start.setEnabled(false);
        btn_stop.setEnabled(false);
        btn_readIntervals.setEnabled(false);
        btn_changeAudioRecordingOn.setEnabled(false);

        tv_deviceName = DataCollectionActivity.this.findViewById(R.id.tv_deviceName);
        tv_imuStatus = DataCollectionActivity.this.findViewById(R.id.tv_imuStatus);
        tv_recordingStatus = DataCollectionActivity.this.findViewById(R.id.tv_recordingStatus);
        tv_audioRecordingOn = DataCollectionActivity.this.findViewById(R.id.tv_audioRecordingOn);
        tv_audioRecordingOn.setText(Boolean.toString(this.audioRecordingOn));

        et_fileName = DataCollectionActivity.this.findViewById(R.id.et_fileName);

        audioRecorderService = new AudioRecorderService(this, mainHandler);

        // get the ESenseManager object and set the device name on screen
        String deviceName = getIntent().getStringExtra("deviceName");
        Binder binder = (Binder) getIntent().getExtras().getBinder("eSenseManagerBinder");
        ESenseManager eSenseManager = (ESenseManager) ((ObjectWrapperForBinder)binder).getData();
        Log.i(TAG, "eSenseManager received = " + eSenseManager);
        tv_deviceName.setText(deviceName);

        imuService = new IMUService(this, mainHandler, deviceName, eSenseManager);
        btn_start.setEnabled(true);
        btn_stop.setEnabled(true);
        btn_readIntervals.setEnabled(true);
        btn_changeAudioRecordingOn.setEnabled(true);
    }

    // Handler object to update the main screen from other threads
    private Handler mainHandler = new Handler() {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 1: // ESenseConfig object received
                    // set the ESenseConfig object
                    imuService.setESenseConfig((ESenseConfig) message.obj);
                    // enable the buttons
//                    btn_start.setEnabled(true);
//                    btn_stop.setEnabled(true);
//                    btn_readIntervals.setEnabled(true);
                    break;
                case 2:
                    tv_imuStatus.setText((String) message.obj);
                    break;
                case 3:
                    tv_recordingStatus.setText((String) message.obj);
                    break;
            }
        };
    };

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_start:
                Log.i(TAG, "btn_startRecording");

                // set the file name using the current time
                Date date = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String fileId = simpleDateFormat.format(date);

                String fileName = et_fileName.getText().toString();
                if (!fileName.equals("")){
                    fileId = fileId + " - " + fileName;
                }

                // start IMU data collection
                if (imuService != null)
                    imuService.startIMU(fileId);
                else
                    Log.i(TAG, "not connected to eSense");

                // start audio recording
                if (this.audioRecordingOn)
                    audioRecorderService.startRecording(fileId);

                break;

            case R.id.btn_stop:
                Log.i(TAG, "btn_stopRecording");

                // stop IMU data collection
                if (imuService != null)
                    imuService.stopIMU();
                else
                    Log.i(TAG, "not connected to eSense");

                // stop audio recording
                if (this.audioRecordingOn)
                    audioRecorderService.stopRecording();

                break;

            case R.id.btn_readIntervals:
                Log.i(TAG, "btn_readIntervals");
                imuService.readIntervals();
                break;

            case R.id.btn_changeAudioRecordingOn:
                Log.i(TAG, "btn_changeAudioRecordingOn");
                this.audioRecordingOn = !this.audioRecordingOn;
                tv_audioRecordingOn.setText(Boolean.toString(this.audioRecordingOn));
        }
    }
}

