package com.lucario.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Toast;

import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.util.List;

public class MainActivity extends AppCompatActivity implements AudioClassificationHelper.sendResult{
    private final String YAMNET_MODEL = "yamnet.tflite";
    private final String SPEECH_COMMAND_MODEL = "speech.tflite";
    private final float DISPLAY_THRESHOLD = 0.3f;
    private final int DEFAULT_NUM_OF_RESULTS = 2;
    private final float DEFAULT_OVERLAP_VALUE = 0.5f;
    private AudioManager audioManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioClassificationHelper audio = new AudioClassificationHelper(getApplicationContext(),
                YAMNET_MODEL, DISPLAY_THRESHOLD, DEFAULT_OVERLAP_VALUE, DEFAULT_NUM_OF_RESULTS,1, 1, this);
    }


    @Override
    public void onResult(List<Classifications> output) {
        if(output.get(0).getCategories().get(0).getLabel().equals("train")){
            System.out.println(output.get(0).getCategories().get(0).getLabel());
            Toast.makeText(MainActivity.this, output.get(0).getCategories().get(0).getLabel(),Toast.LENGTH_SHORT).show();
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 30, AudioManager.FLAG_SHOW_UI);
        System.out.println( output.get(0).getCategories().get(0).getLabel());
        Toast.makeText(MainActivity.this, output.get(0).getCategories().get(0).getLabel(),Toast.LENGTH_SHORT).show();
    }
}