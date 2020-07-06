package com.example.esenseapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.esenseapplication.services.ConnectService;
import com.example.esenseapplication.services.ScanService;

import io.esense.esenselib.ESenseManager;

public class ConnectActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "ConnectActivity";

    Button btn_connect, btn_nextStep;
    TextView tv_deviceName, tv_connectStatus;

    ConnectService connectService;

    // device name of eSense
    String deviceName;

    // ESenseManager object, to be set when connected to eSense
    ESenseManager eSenseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        btn_connect = ConnectActivity.this.findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(this);
        btn_nextStep = ConnectActivity.this.findViewById(R.id.btn_nextStep);
        btn_nextStep.setOnClickListener(this);
        btn_nextStep.setEnabled(false);

        tv_deviceName = ConnectActivity.this.findViewById(R.id.tv_deviceName);
        tv_connectStatus = ConnectActivity.this.findViewById(R.id.tv_connectStatus);

        // get the device name and set it on screen
        deviceName = getIntent().getStringExtra("deviceName");
        tv_deviceName.setText(deviceName);

        connectService = new ConnectService(this, mainHandler, deviceName);
    }

    // Handler object to update UI from other threads
    private Handler mainHandler = new Handler() {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 1: // state changed
                    tv_connectStatus.setText((String) message.obj);
                    break;
                case 2: // connected
                    // get the ESenseManager object
                    eSenseManager = (ESenseManager) message.obj;
                    Log.i(TAG, "reading sensor config");
                    boolean b = eSenseManager.getSensorConfig();
                    // set status and enable/disable buttons
                    tv_connectStatus.setText("connected");
                    btn_connect.setEnabled(false);
                    btn_nextStep.setEnabled(true);
                    break;
            }
        };
    };

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_connect:
                Log.i(TAG, "btn_connect");
                connectService.connect();
                break;
            case R.id.btn_nextStep:
                Log.i(TAG, "btn_nextStep");
                // set the next activity to jump to
                Intent intent = new Intent(ConnectActivity.this, DataCollectionActivity.class);
                // disable returning to this activity
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                // pass the device name
                intent.putExtra("deviceName", deviceName);
                // pass the eSenseManager object
                Bundle bundle = new Bundle();
                bundle.putBinder("eSenseManagerBinder", new ObjectWrapperForBinder(eSenseManager));
                intent.putExtras(bundle);
                // jump to the next activity
                startActivity(intent);
                Log.d(TAG, "eSenseManager sent = " + eSenseManager);
                break;
        }
    }
}
