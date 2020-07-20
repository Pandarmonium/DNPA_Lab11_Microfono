package com.example.dons.dnpa_lab11;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import android.media.MediaRecorder.AudioSource;


public class MainActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSION_CODE = 101;
    private final int SAMPLINGRATE_IN_HZ = 44100;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE_FACTOR = 2;
    private final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLINGRATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    private boolean mRecording;
    private String pathSave = "";
    private Calendar calendar;

    private AudioRecord audioRecord = null;
    private AudioTrack audioTrack;
    private Thread recordingThread = null;

    private Button btnStartRecord;
    private Button btnStopRecord;
    private Button btnPlayAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasMicrophone(this))
            return;
        checkPermissionsFromDevice();

        btnStartRecord = (Button) findViewById(R.id.btnStartRecord);
        btnStopRecord = (Button) findViewById(R.id.btnStopRecord);
        btnPlayAudio = (Button) findViewById(R.id.btnPlayAudio);

        //Start Record
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("ddMMMyyyy_HHmmss");
                String formatDate = df.format(calendar.getTime());

                pathSave= Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/" + formatDate + "_record.pcm";

                startRecording();
                btnPlayAudio.setEnabled(false);
                btnStopRecord.setEnabled(true);
                btnStartRecord.setEnabled(false);

            }
        });

        btnStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                btnStartRecord.setEnabled(true);
                btnStopRecord.setEnabled(false);
                btnPlayAudio.setEnabled(true);
            }
        });

        btnPlayAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecordAudio();
            }
        });
    }

    private void startRecording() {
        audioRecord = new AudioRecord(AudioSource.DEFAULT, SAMPLINGRATE_IN_HZ, CHANNEL_CONFIG,
                AUDIO_FORMAT, BUFFER_SIZE);

        audioRecord.startRecording();
        mRecording = true;

        Toast.makeText(MainActivity.this, "Recording...", Toast.LENGTH_SHORT).show();

        recordingThread = new Thread(new RecordingRunnable(),"Recording Thread");
        recordingThread.start();

    }

    private void stopRecording() {
        if (audioRecord == null)
            return;

        mRecording = false;
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        recordingThread = null;
    }

    private class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            final File audioFile = new File(pathSave);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outputStream = new FileOutputStream(audioFile)){
                while (mRecording){
                    int result = audioRecord.read(buffer, BUFFER_SIZE);
                    if (result < 0)
                        throw new RuntimeException("Reading of audio buffer failed: "+ getBufferReadFailureReason(result));

                    outputStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private String getBufferReadFailureReason(int errorCode) {
        switch (errorCode) {
            case AudioRecord.ERROR_INVALID_OPERATION:
                return "ERROR_INVALID_OPERATION";
            case AudioRecord.ERROR_BAD_VALUE:
                return "ERROR_BAD_VALUE";
            case AudioRecord.ERROR_DEAD_OBJECT:
                return "ERROR_DEAD_OBJECT";
            case AudioRecord.ERROR:
                return "ERROR";
            default:
                return "Unknown (" + errorCode + ")";
        }
    }

    private void playRecordAudio() {
        int buffSize = AudioTrack.getMinBufferSize(SAMPLINGRATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO
                , AUDIO_FORMAT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLINGRATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO
                , AUDIO_FORMAT, buffSize, AudioTrack.MODE_STREAM);

        Toast.makeText(MainActivity.this,"Playing...", Toast.LENGTH_SHORT).show();

        try {
            FileInputStream fileInputStream = new FileInputStream(pathSave);
            byte[] buffer = new byte[buffSize];
            audioTrack.play();
            try{
                while (fileInputStream.read(buffer) > 0){
                    audioTrack.write(buffer, 0, buffer.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(getApplicationContext(),"Read and Write permission is necessary",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void checkPermissionsFromDevice() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_PERMISSION_CODE);
            return;
        }

    }

    public static boolean hasMicrophone(Context context)
    {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }
}