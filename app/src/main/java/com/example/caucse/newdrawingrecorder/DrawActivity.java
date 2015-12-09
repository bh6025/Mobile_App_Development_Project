package com.example.caucse.newdrawingrecorder;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import me.panavtec.drawableview.DrawableView;
import me.panavtec.drawableview.DrawableViewConfig;

/**
 * Created by CAUCSE on 2015-12-06.
 */
public class DrawActivity extends Activity {

    private DrawableView drawableView;
    private DrawableViewConfig config = new DrawableViewConfig();
    private final int REQ_CODE_GALLERY=100;

/////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    private MediaRecorder mMediaRecorder;
    private String mFileName = null;

    public int i =0;
    /////////////////////////////////////////////////////////////////////////////////////////////////

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        initUi();


        /////////////////////////////////////////////////////////////////////////////////////////////////
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mMediaRecorder = new MediaRecorder();
        initRecorder();
        prepareRecorder();
        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);
        mToggleButton = (ToggleButton) findViewById(R.id.startButton);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleScreenShare(v);
            }
        });
        mMediaProjectionCallback = new MediaProjectionCallback();
        /////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private void initUi() {
        drawableView = (DrawableView) findViewById(R.id.paintView);
        Button strokeWidthMinusButton = (Button) findViewById(R.id.strokeWidthMinusButton);
        Button strokeWidthPlusButton = (Button) findViewById(R.id.strokeWidthPlusButton);
        Button changeColorButton = (Button) findViewById(R.id.changeColorButton);
        Button undoButton = (Button) findViewById(R.id.undoButton);
        Button loadButton = (Button) findViewById(R.id.loadButton);
        ToggleButton startButton = (ToggleButton) findViewById(R.id.startButton);



        config.setStrokeColor(getResources().getColor(android.R.color.black));
        config.setShowCanvasBounds(true);
        config.setStrokeWidth(20.0f);
        config.setMinZoom(1.0f);
        config.setMaxZoom(3.0f);
        config.setCanvasHeight(1600);
        config.setCanvasWidth(1080);
        drawableView.setConfig(config);

        strokeWidthPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                config.setStrokeWidth(config.getStrokeWidth() + 10);
            }
        });
        strokeWidthMinusButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                config.setStrokeWidth(config.getStrokeWidth() - 10);
            }
        });
        changeColorButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if((i%5)==0) config.setStrokeColor(Color.argb(255, 255, 0, 0));
                else if((i%5)==1) config.setStrokeColor(Color.argb(255, 0, 255, 0));
                else if((i%5)==2) config.setStrokeColor(Color.argb(255, 0, 0, 255));
                else if((i%5)==3) config.setStrokeColor(Color.argb(255, 255, 255, 255));
                else if((i%5)==4) config.setStrokeColor(Color.argb(255, 0, 0, 0));
                i++;
            }
        });

        undoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                drawableView.undo();
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                final Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQ_CODE_GALLERY);
            }
        });

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode == REQ_CODE_GALLERY && resultCode == RESULT_OK && null != intent) {

            final Uri selectImageUri = intent.getData();
            final String[] filePathColumn = {MediaStore.Images.Media.DATA};

            final Cursor imageCursor= this.getContentResolver().query(selectImageUri, filePathColumn, null, null, null);
            imageCursor.moveToFirst();

            final int columnIndex = imageCursor.getColumnIndex(filePathColumn[0]);
            final String imagePath = imageCursor.getString(columnIndex);
            imageCursor.close();

            final Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

            Drawable d = new BitmapDrawable(getResources(), bitmap);
            drawableView.setBackground(d);

            drawableView.invalidate();
        }


        /////////////////////////////////////////////////////////////////////////////////////////////////
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }
        if(requestCode == 1) {
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, intent);
            mMediaProjection.registerCallback(mMediaProjectionCallback, null);
            mVirtualDisplay = createVirtualDisplay();
            mMediaRecorder.start();
        }

    }

    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            shareScreen();
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");
            stopScreenSharing();
            //initRecorder();
            //prepareRecorder();
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("DrawActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
                //initRecorder();
                //prepareRecorder();
            }
            mMediaProjection = null;
            stopScreenSharing();
            Log.i(TAG, "MediaProjection Stopped");
        }
    }

    private void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void initRecorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/pictures/DrawingRecorder/" + new SimpleDateFormat("yyyyMMdd__HHmmss").format(new Date()) + "record.mp4";
        mMediaRecorder.setOutputFile(mFileName);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////
}