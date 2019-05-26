package com.hg.hollowgoods.recorder;

import android.media.MediaPlayer;

import com.hg.hollowgoods.recorder.model.AudioChannel;
import com.hg.hollowgoods.recorder.model.AudioSampleRate;
import com.hg.hollowgoods.recorder.model.AudioSource;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;

public class AudioRecorderUtils implements PullTransport.OnAudioChunkPulledListener, MediaPlayer.OnCompletionListener {

    private String filePath;
    private String filename;
    private AudioSource source;
    private AudioChannel channel;
    private AudioSampleRate sampleRate;

    private MediaPlayer player;
    private Recorder recorder;
//    private VisualizerHandler visualizerHandler;

    private Timer timer;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;
    private boolean isRecording;

    public AudioRecorderUtils(String filePath, AudioSource source, AudioChannel channel, AudioSampleRate sampleRate) {
        this.filePath = filePath;
        this.source = source;
        this.channel = channel;
        this.sampleRate = sampleRate;
    }

//    public void onPause() {
//        restartRecording();
//    }
//
//    public void onDestroy() {
//        restartRecording();
//    }

    @Override
    public void onAudioChunkPulled(AudioChunk audioChunk) {
        float amplitude = isRecording ? (float) audioChunk.maxAmplitude() : 0f;
//        visualizerHandler.onDataReceived(amplitude);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopPlaying();
    }

    public void toggleRecording() {

        stopPlaying();
        HelperUtils.wait(100, () -> {
            if (isRecording) {
                pauseRecording();
            } else {
                resumeRecording();
            }
        });
    }

    public void togglePlaying() {

        pauseRecording();
        HelperUtils.wait(100, () -> {
            if (isPlaying()) {
                stopPlaying();
            } else {
                startPlaying();
            }
        });
    }

    public void restartRecording() {

        if (isRecording) {
            stopRecording();
        } else if (isPlaying()) {
            stopPlaying();
        } else {
//            visualizerHandler = new VisualizerHandler();
//            visualizerHandler.stop();
        }

        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;
    }

    public void resumeRecording() {

        isRecording = true;

//        visualizerHandler = new VisualizerHandler();

        if (recorder == null) {
            initFilename();
            recorder = OmRecorder.wav(
                    new PullTransport.Default(HelperUtils.getMic(source, channel, sampleRate), AudioRecorderUtils.this),
                    new File(filePath, filename));
        }
        recorder.resumeRecording();

        startTimer();
    }

    public void pauseRecording() {

        isRecording = false;

//        if (visualizerHandler != null) {
//            visualizerHandler.stop();
//        }

        if (recorder != null) {
            recorder.pauseRecording();
        }

        stopTimer();
    }

    public void stopRecording() {

//        if (visualizerHandler != null) {
//            visualizerHandler.stop();
//        }

        recorderSecondsElapsed = 0;
        if (recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }

        stopTimer();
    }

    public void startPlaying() {
        try {
            stopRecording();
            player = new MediaPlayer();
            player.setDataSource(filePath + filename);
            player.prepare();
            player.start();

            playerSecondsElapsed = 0;
            startTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPlaying() {

//        if (visualizerHandler != null) {
//            visualizerHandler.stop();
//        }

        if (player != null) {
            try {
                player.stop();
                player.reset();
            } catch (Exception ignored) {

            }
        }

        stopTimer();
    }

    public boolean isPlaying() {
        try {
            return player != null && player.isPlaying() && !isRecording;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void updateTimer() {
        if (isRecording) {
            recorderSecondsElapsed++;
        } else if (isPlaying()) {
            playerSecondsElapsed++;
        }
    }

    private void initFilename() {
        filename = System.currentTimeMillis() + ".wav";
    }

    public int getRecorderSecondsElapsed() {
        return recorderSecondsElapsed;
    }

    public int getPlayerSecondsElapsed() {
        return playerSecondsElapsed;
    }

    public String getFilename() {
        return filename;
    }
}
