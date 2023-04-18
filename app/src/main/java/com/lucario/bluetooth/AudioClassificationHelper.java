package com.lucario.bluetooth;


import android.content.Context;
import android.media.AudioRecord;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.core.BaseTaskApi;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class AudioClassificationHelper {

    private final int DELEGATE_CPU = 0;
    private final int DELEGATE_NNAPI = 1;
    private final float DISPLAY_THRESHOLD = 0.3f;
    private final int DEFAULT_NUM_OF_RESULTS = 2;
    private final float DEFAULT_OVERLAP_VALUE = 0.5f;
    private final String YAMNET_MODEL = "yamnet.tflite";
    private final String SPEECH_COMMAND_MODEL = "speech.tflite";
    Context context;
    String currentModel;
    float classificationThreshold, overlap;
    int numOfResults, currentDelegate, numThreads;

    public AudioClassificationHelper(Context context, String currentModel, float classificationThreshold, float overlap, int numOfResults, int currentDelegate, int numThreads) {
        this.context = context;
        this.currentModel = currentModel;
        this.classificationThreshold = classificationThreshold;
        this.overlap = overlap;
        this.numOfResults = numOfResults;
        this.currentDelegate = currentDelegate;
        this.numThreads = numThreads;

    }

    private AudioClassifier classifier;
    private TensorAudio tensorAudio;
    private AudioRecord recorder;
    private ScheduledThreadPoolExecutor executor;

    private final Runnable classifyRunnable = new Runnable() {
        @Override
        public void run() {
            classifyAudio();
        }
    };

    private void initClassifier(){
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder()
                .setNumThreads(numThreads);

        switch(currentDelegate){
            case DELEGATE_CPU:
            case DELEGATE_NNAPI:
                baseOptionsBuilder.useNnapi();
        }

        AudioClassifier.AudioClassifierOptions options = AudioClassifier.AudioClassifierOptions.builder()
                .setScoreThreshold(classificationThreshold)
                .setMaxResults(numOfResults)
                .setBaseOptions(BaseOptions.builder().build())
                .build();

        try {
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options);
            tensorAudio = classifier.createInputTensorAudio();
            recorder = classifier.createAudioRecord();
            startAudioClassification();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startAudioClassification(){
        if(recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
            return;
        }
        recorder.startRecording();
        executor = new ScheduledThreadPoolExecutor(1);

    }

    private void classifyAudio(){}
}
