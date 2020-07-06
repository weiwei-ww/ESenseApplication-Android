package com.example.esenseapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.esenseapplication.services.ScanService;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "ScanActivity";

    Button btn_startScan, btn_stopScan, btn_nextStep;
    TextView tv_deviceName, tv_scanStatus;

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.BROADCAST_STICKY,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    ScanService scanService;

    // device name of eSense, to be set when an eSense device is found
    String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        btn_startScan = ScanActivity.this.findViewById(R.id.btn_startScan);
        btn_startScan.setOnClickListener(this);
        btn_stopScan = ScanActivity.this.findViewById(R.id.btn_stopScan);
        btn_stopScan.setOnClickListener(this);
        btn_nextStep = ScanActivity.this.findViewById(R.id.btn_nextStep);
        btn_nextStep.setOnClickListener(this);
        btn_nextStep.setEnabled(false);

        tv_deviceName = ScanActivity.this.findViewById(R.id.tv_deviceName);
        tv_scanStatus = ScanActivity.this.findViewById(R.id.tv_scanStatus);

        scanService = new ScanService(this, mainHandler);

        requestPermissions(permissions);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_startScan:
                Log.i(TAG, "btn_startScan");
                scanService.startScan();
                break;
            case R.id.btn_stopScan:
                Log.i(TAG, "btn_stopScan");
                scanService.stopScan();
                break;
            case R.id.btn_nextStep:
                Log.i(TAG, "btn_nextStep");
                // set the next activity to jump to
                Intent intent = new Intent(ScanActivity.this, ConnectActivity.class);
                // disable returning to this activity
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                // pass the device name
                intent.putExtra("deviceName", deviceName);
                // jump to the next activity
                startActivity(intent);
                break;
        }
    }

    // Handler object to update UI from other threads
     private Handler mainHandler = new Handler() {
         public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 1: // state changed
                    tv_scanStatus.setText((String) message.obj);
                    break;
                case 2: // device found
                    // get the device name
                    deviceName = (String) message.obj;
                    // set status and enable/disable buttons
                    tv_scanStatus.setText("device found (" + deviceName + ")");
                    btn_startScan.setEnabled(false);
                    btn_stopScan.setEnabled(false);
                    btn_nextStep.setEnabled(true);
                    break;
            }
        };
    };

    private void requestPermissions(String[] permissions){
        Log.i(TAG, "requestPermissions()");

        for (String permission : permissions){
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{permission}, 1);
        }
    }
}
