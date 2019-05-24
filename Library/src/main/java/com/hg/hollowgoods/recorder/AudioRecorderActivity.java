package com.hg.hollowgoods.recorder;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cleveroad.audiovisualization.DbmHandler;
import com.cleveroad.audiovisualization.GLAudioVisualizationView;
import com.hg.hollowgoods.UI.Base.BaseActivity;
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

public class AudioRecorderActivity extends BaseActivity
        implements PullTransport.OnAudioChunkPulledListener, MediaPlayer.OnCompletionListener {

    private String filePath;
    private AudioSource source;
    private AudioChannel channel;
    private AudioSampleRate sampleRate;
    private int color;
    private boolean autoStart;
    private boolean keepDisplayOn;

    private MediaPlayer player;
    private Recorder recorder;
    private VisualizerHandler visualizerHandler;

    private Timer timer;
    private MenuItem saveMenuItem;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;
    private boolean isRecording;

    private RelativeLayout contentLayout;
    private GLAudioVisualizationView visualizerView;
    private TextView statusView;
    private TextView timerView;
    private ImageButton restartView;
    private ImageButton recordView;
    private ImageButton playView;

    @Override
    public Activity addToExitGroup() {
        return this;
    }

    @Override
    public int bindLayout() {
        return R.layout.aar_activity_audio_recorder;
    }

    @Nullable
    @Override
    public Object initView(View view, Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            filePath = savedInstanceState.getString(AndroidAudioRecorder.EXTRA_FILE_PATH);
            source = (AudioSource) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_SOURCE);
            channel = (AudioChannel) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_CHANNEL);
            sampleRate = (AudioSampleRate) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
            color = savedInstanceState.getInt(AndroidAudioRecorder.EXTRA_COLOR);
            autoStart = savedInstanceState.getBoolean(AndroidAudioRecorder.EXTRA_AUTO_START);
            keepDisplayOn = savedInstanceState.getBoolean(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON);
        } else {
            filePath = getIntent().getStringExtra(AndroidAudioRecorder.EXTRA_FILE_PATH);
            source = (AudioSource) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SOURCE);
            channel = (AudioChannel) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_CHANNEL);
            sampleRate = (AudioSampleRate) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
            color = getIntent().getIntExtra(AndroidAudioRecorder.EXTRA_COLOR, Color.BLACK);
            autoStart = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_AUTO_START, false);
            keepDisplayOn = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON, false);
        }

        if (keepDisplayOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setElevation(0);
            int darkColor = HelperUtils.getDarkerColor(color);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(darkColor));
            getWindow().setStatusBarColor(darkColor);
            getSupportActionBar().setHomeAsUpIndicator(
                    ContextCompat.getDrawable(this, R.drawable.aar_ic_clear));
        }

        visualizerView = new GLAudioVisualizationView.Builder(this)
                .setLayersCount(1)
                .setWavesCount(6)
                .setWavesHeight(R.dimen.aar_wave_height)
                .setWavesFooterHeight(R.dimen.aar_footer_height)
                .setBubblesPerLayer(20)
                .setBubblesSize(R.dimen.aar_bubble_size)
                .setBubblesRandomizeSize(true)
                .setBackgroundColor(HelperUtils.getDarkerColor(color))
                .setLayerColors(new int[]{color})
                .build();

        contentLayout = findViewById(R.id.content);
        statusView = findViewById(R.id.status);
        timerView = findViewById(R.id.timer);
        restartView = findViewById(R.id.restart);
        recordView = findViewById(R.id.record);
        playView = findViewById(R.id.play);

        contentLayout.setBackgroundColor(HelperUtils.getDarkerColor(color));
        contentLayout.addView(visualizerView, 0);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);

        if (HelperUtils.isBrightColor(color)) {
            ContextCompat.getDrawable(this, R.drawable.aar_ic_clear)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            ContextCompat.getDrawable(this, R.drawable.aar_ic_check)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            statusView.setTextColor(Color.BLACK);
            timerView.setTextColor(Color.BLACK);
            restartView.setColorFilter(Color.BLACK);
            recordView.setColorFilter(Color.BLACK);
            playView.setColorFilter(Color.BLACK);
        }

        return null;
    }

    @Override
    public void setListener() {

    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (autoStart && !isRecording) {
            toggleRecording(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            visualizerView.onResume();
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void onPause() {
        restartRecording(null);
        try {
            visualizerView.onPause();
        } catch (Exception ignored) {

        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        restartRecording(null);
        setResult(RESULT_CANCELED);
        try {
            visualizerView.release();
        } catch (Exception ignored) {

        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(AndroidAudioRecorder.EXTRA_FILE_PATH, filePath);
        outState.putInt(AndroidAudioRecorder.EXTRA_COLOR, color);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aar_audio_recorder, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        saveMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.aar_ic_check));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
        } else if (i == R.id.action_save) {
            selectAudio();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAudioChunkPulled(AudioChunk audioChunk) {
        float amplitude = isRecording ? (float) audioChunk.maxAmplitude() : 0f;
        visualizerHandler.onDataReceived(amplitude);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopPlaying();
    }

    private void selectAudio() {
        stopRecording();
        setResult(RESULT_OK);
        finish();
    }

    public void toggleRecording(View v) {
        stopPlaying();
        HelperUtils.wait(100, () -> {
            if (isRecording) {
                pauseRecording();
            } else {
                resumeRecording();
            }
        });
    }

    public void togglePlaying(View v) {
        pauseRecording();
        HelperUtils.wait(100, () -> {
            if (isPlaying()) {
                stopPlaying();
            } else {
                startPlaying();
            }
        });
    }

    public void restartRecording(View v) {
        if (isRecording) {
            stopRecording();
        } else if (isPlaying()) {
            stopPlaying();
        } else {
            visualizerHandler = new VisualizerHandler();
            visualizerView.linkTo(visualizerHandler);
            visualizerView.release();
            if (visualizerHandler != null) {
                visualizerHandler.stop();
            }
        }
        saveMenuItem.setVisible(false);
        statusView.setVisibility(View.INVISIBLE);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);
        recordView.setImageResource(R.drawable.aar_ic_rec);
        timerView.setText("00:00:00");
        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;
    }

    private void resumeRecording() {
        isRecording = true;
        saveMenuItem.setVisible(false);
        statusView.setText(R.string.aar_recording);
        statusView.setVisibility(View.VISIBLE);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);
        recordView.setImageResource(R.drawable.aar_ic_pause);
        playView.setImageResource(R.drawable.aar_ic_play);

        visualizerHandler = new VisualizerHandler();
        visualizerView.linkTo(visualizerHandler);

        if (recorder == null) {
            timerView.setText("00:00:00");

            recorder = OmRecorder.wav(
                    new PullTransport.Default(HelperUtils.getMic(source, channel, sampleRate), AudioRecorderActivity.this),
                    new File(filePath));
        }
        recorder.resumeRecording();

        startTimer();
    }

    private void pauseRecording() {
        isRecording = false;
        if (!isFinishing()) {
            saveMenuItem.setVisible(true);
        }
        statusView.setText(R.string.aar_paused);
        statusView.setVisibility(View.VISIBLE);
        restartView.setVisibility(View.VISIBLE);
        playView.setVisibility(View.VISIBLE);
        recordView.setImageResource(R.drawable.aar_ic_rec);
        playView.setImageResource(R.drawable.aar_ic_play);

        visualizerView.release();
        if (visualizerHandler != null) {
            visualizerHandler.stop();
        }

        if (recorder != null) {
            recorder.pauseRecording();
        }

        stopTimer();
    }

    private void stopRecording() {
        visualizerView.release();
        if (visualizerHandler != null) {
            visualizerHandler.stop();
        }

        recorderSecondsElapsed = 0;
        if (recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }

        stopTimer();
    }

    private void startPlaying() {
        try {
            stopRecording();
            player = new MediaPlayer();
            player.setDataSource(filePath);
            player.prepare();
            player.start();

            visualizerView.linkTo(DbmHandler.Factory.newVisualizerHandler(this, player));
            visualizerView.post(() -> player.setOnCompletionListener(AudioRecorderActivity.this));

            timerView.setText("00:00:00");
            statusView.setText(R.string.aar_playing);
            statusView.setVisibility(View.VISIBLE);
            playView.setImageResource(R.drawable.aar_ic_stop);

            playerSecondsElapsed = 0;
            startTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        statusView.setText("");
        statusView.setVisibility(View.INVISIBLE);
        playView.setImageResource(R.drawable.aar_ic_play);

        visualizerView.release();
        if (visualizerHandler != null) {
            visualizerHandler.stop();
        }

        if (player != null) {
            try {
                player.stop();
                player.reset();
            } catch (Exception ignored) {

            }
        }

        stopTimer();
    }

    private boolean isPlaying() {
        try {
            return player != null && player.isPlaying() && !isRecording;
        } catch (Exception e) {
            return false;
        }
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
        runOnUiThread(() -> {
            if (isRecording) {
                recorderSecondsElapsed++;
                timerView.setText(HelperUtils.formatSeconds(recorderSecondsElapsed));
            } else if (isPlaying()) {
                playerSecondsElapsed++;
                timerView.setText(HelperUtils.formatSeconds(playerSecondsElapsed));
            }
        });
    }

}
