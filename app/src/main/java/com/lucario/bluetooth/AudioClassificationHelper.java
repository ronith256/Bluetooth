package com.lucario.bluetooth;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;
import org.tensorflow.lite.task.core.BaseOptions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AudioClassificationHelper {

    private final int DELEGATE_CPU = 0;
    private final int DELEGATE_NNAPI = 1;
    private final String channelId = "traffic";
    private NotificationManagerCompat notificationManager;
    Context context;
    String currentModel;
    float classificationThreshold, overlap;
    private final sendResult listener;
    int numOfResults, currentDelegate, numThreads;

    private AudioManager audioManager;
    public AudioClassificationHelper(Context context, String currentModel, float classificationThreshold, float overlap, int numOfResults, int currentDelegate, int numThreads, sendResult listener) {
        this.context = context;
        this.currentModel = currentModel;
        this.classificationThreshold = classificationThreshold;
        this.overlap = overlap;
        this.listener = listener;
        this.numOfResults = numOfResults;
        this.currentDelegate = currentDelegate;
        this.numThreads = numThreads;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        createChannel();
//        sendNearbyNotification("Test");
        initClassifier();

    }

    private AudioClassifier classifier;
    private TensorAudio tensorAudio;
    private AudioRecord recorder;
    private ScheduledThreadPoolExecutor executor;

    private final Runnable classifyRunnable = this::classifyAudio;

    private void initClassifier(){
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder()
                .setNumThreads(numThreads);

        switch(currentDelegate){
            case DELEGATE_CPU:
                baseOptionsBuilder.useNnapi();
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
            Log.e("AudioClassification", "TFLite failed to load with error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void startAudioClassification(){
        if(recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
            return;
        }
        recorder.startRecording();
        executor = new ScheduledThreadPoolExecutor(1);

        float lengthInMilliSeconds = ((classifier.getRequiredInputBufferSize() * 1.0f) /
                classifier.getRequiredTensorAudioFormat().getSampleRate()) * 1000;
        long interval = (long) (lengthInMilliSeconds * (1 - overlap));

        executor.scheduleAtFixedRate(
                classifyRunnable,
                0,
                interval,
                TimeUnit.MILLISECONDS
        );
    }
    private void classifyAudio(){
        tensorAudio.load(recorder);
        long inferenceTime = SystemClock.uptimeMillis();
        List<Classifications> output = classifier.classify(tensorAudio);
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
        printList(output);
    }

    private boolean printList(List<Classifications> output){
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // get the current music volume level
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // get the maximum music volume level
        float volumeLevel = 0.5f; // set the desired volume level here (range: 0.0f to 1.0f)
        int newVolume = (int) (maxVolume * volumeLevel);
        for(int i = 0; i < output.size(); i++){
            for(int j = 0; j < output.get(i).getCategories().size(); j++){
                if(output.get(i).getCategories().get(j).getIndex() == 57){
                    sendNearbyNotification("Traffic Detected");
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                    return true;
                }
            }
        }
        return false;
    }

    private void stopAudioClassification(){
        recorder.stop();
        executor.shutdownNow();
    }

    public interface sendResult{
        void onResult(List<Classifications> output);
    }

    private void sendNearbyNotification(String name) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Warning")
                .setContentText(name)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        // notificationId is a unique int for each notification that you must define
        int notificationId = name.hashCode();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationManager.notify(notificationId, builder.build());
    }

    private void createChannel(){
        notificationManager = NotificationManagerCompat.from(context);
        CharSequence channelName = "Traffic";
        String description = "This is used to send you warnings";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }
}
