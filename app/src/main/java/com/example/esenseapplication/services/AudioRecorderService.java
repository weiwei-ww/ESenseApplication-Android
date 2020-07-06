package com.example.esenseapplication.services;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class AudioRecorderService {
    String TAG = "AudioRecorderService";
    Context context;
    // Handler object to update the main screen
    Handler mainHandler;

    MediaRecorder mediaRecorder;
    AudioManager audioManager;

    String fileId;

    RecorderState recorderState = RecorderState.STOPPED;
    public enum RecorderState {
        STOPPED, RECORDING, ERROR;

        public String toString(){
            switch (this){
                case STOPPED:
                    return "stopped";
                case RECORDING:
                    return "recording";
                case ERROR:
                    return "error";
            }
            return null;
        }
    }

    public AudioRecorderService(Context context, Handler mainHandler){
        Log.i(TAG, "AudioRecorderService");
        this.context = context;
        this.mainHandler = mainHandler;

        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        // check and request permission
        requestPermission();
    }

    private void setStatus(RecorderState recorderState) {
        this.recorderState = recorderState;
        // update the main screen
        Message message = new Message();
        message.what = 3;
        message.obj = recorderState.toString();
        mainHandler.sendMessage(message);
    }

    public void startRecording(String fileId) {
        Log.i(TAG, "startRecording()");

        this.fileId = fileId;

        try {
            if (recorderState != RecorderState.RECORDING) {
                // set the dir to save the recordings
                String dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
                // set the file name
                String fileName = fileId + ".aac";
                File recordingFile = new File(dir, fileName);
                Log.i(TAG, "Recording will be saved to: " + recordingFile.getAbsolutePath());

                // initialize the MediaRecorder object
                mediaRecorder = new MediaRecorder();
                // set the source
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                // set the output format
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                // set the audio encoder
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                // set the output file name
                mediaRecorder.setOutputFile(recordingFile);
                // prepare the recorder
                mediaRecorder.prepare();

//                // start recording
//                mediaRecorder.start();
//                // set state
//                setStatus(RecorderState.RECORDING);

                // start SCO for Bluetooth
                audioManager.stopBluetoothSco();
                audioManager.startBluetoothSco();

                // start recordings when SCO is successfully started
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.i(TAG, "AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED");

                        int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                        Log.i(TAG, "SCO state: " + state);

                        if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED){
                            Log.i(TAG, "SCO connected");

                            // open SCO
                            audioManager.setBluetoothScoOn(true);
                            Log.i(TAG, "Using Bluetooth: " + audioManager.isBluetoothScoOn());
                            // set AudioManager mode
                            audioManager.setMode(audioManager.MODE_NORMAL);
                            // start recording
                            mediaRecorder.start();
                            // set state
                            setStatus(RecorderState.RECORDING);

                            // unregister
                            context.unregisterReceiver(this);
                        }
                        else {
                            Log.i(TAG, "SCO not connected, wait for 1 sec");

                            // wait for 1 sec
                            try {
                                Thread.sleep(1000);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                                setStatus(RecorderState.ERROR);
                            }
                            // start SCO again
                            audioManager.startBluetoothSco();
                        }
                    }
                }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            }
        }
        catch (Exception e){
            e.printStackTrace();
            setStatus(RecorderState.ERROR);
        }
    }

    public void stopRecording(){
        Log.i(TAG, "stopRecording()");

        try {
            if (mediaRecorder != null) {
                // stop recording and release the MediaRecorder object
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            // set state
            setStatus(RecorderState.STOPPED);
        }
        catch (Exception e){
            e.printStackTrace();
            setStatus(RecorderState.ERROR);
        }
    }

    private void requestPermission(){
        Log.i(TAG, "requestPermission()");

        if (ContextCompat.checkSelfPermission(context.getApplicationContext(), RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context.getApplicationContext(), WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions((Activity) context, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1);
    }
}
