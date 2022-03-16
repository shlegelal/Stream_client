package com.example.stream_client;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends Activity implements
        OnClickListener,
        RtspClient.Callback,
        Session.Callback,
        SurfaceHolder.Callback {

    private final static String TAG = "MainActivity";

    private Button mButton_start, mButton_swap;
    private SurfaceView mSurfaceView;
    private EditText mEditText;
    private Session mSession;
    private RtspClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mButton_start = findViewById(R.id.button_start);
        mButton_swap = findViewById(R.id.button_swap);
        mSurfaceView = findViewById(R.id.surface);
        mEditText = findViewById(R.id.editTextIp);

        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setAudioQuality(new AudioQuality(16000, 32000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(0)
                .setCallback(this)
                //.setVideoQuality(new VideoQuality(320,240,20,500000))
                .build();

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);

        mButton_start.setOnClickListener(this);
        mButton_swap.setOnClickListener(this);

        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mClient.isStreaming()) {
            mButton_start.setText(R.string.stop);
        } else {
            mButton_start.setText(R.string.start);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.release();
        mSession.release();
        mSurfaceView.getHolder().removeCallback(this);
    }

    @Override
    public void onClick(View v) {
        mSession.setDestination(mEditText.getText().toString());
        if (v.getId() == R.id.button_start) {
            // Starts/stops streaming
            toggleStream();
            mButton_start.setEnabled(false);
        } else {
            // Switch between the two cameras
            mSession.switchCamera();
        }
    }

    // Connects/disconnects to the RTSP server and starts/stops the stream
    public void toggleStream() {
        if (!mClient.isStreaming()) {
            String ip = mEditText.getText().toString();
            int port = 8086;

            mClient.setServerAddress(ip, port);
            mClient.setStreamPath("/");
            mClient.startStream();

        } else {
            // Stops the stream and disconnects from the RTSP server
            mClient.stopStream();
        }
    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        Log.d(TAG,"Bitrate: "+bitrate);
    }

    @Override
    public void onSessionError(int message, int streamType, Exception e) {
        mButton_start.setEnabled(true);
        if (e != null) {
            logError(e.getMessage());
        }
    }

    @Override

    public void onPreviewStarted() {
        Log.d(TAG,"Preview started.");
    }

    @Override
    public void onSessionConfigured() {
        Log.d(TAG,"Preview configured.");
        // Once the stream is configured, you can get a SDP formated session description
        // that you can send to the receiver of the stream.
        // For example, to receive the stream in VLC, store the session description in a .sdp file
        // and open it with VLC while streming.
        Log.d(TAG, mSession.getSessionDescription());
        mSession.start();
    }

    @Override
    public void onSessionStarted() {
        Log.d(TAG,"Session started.");
        mButton_start.setEnabled(true);
        mButton_start.setText(R.string.stop);
    }

    @Override
    public void onSessionStopped() {
        Log.d(TAG,"Session stopped.");
        mButton_start.setEnabled(true);
        mButton_start.setText(R.string.start);
    }

    /** Displays a popup to report the error to the user */
    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(error).setPositiveButton("OK", (dialog, id) -> {});
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRtspUpdate(int message, Exception e) {
        switch (message) {
            case RtspClient.ERROR_CONNECTION_FAILED:
            case RtspClient.ERROR_WRONG_CREDENTIALS:
                e.printStackTrace();
                break;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSession.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSession.stop();
    }

}
