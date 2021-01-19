package com.tokbox.sample.arcoreintegration;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.view.PixelCopy;
import com.google.ar.sceneform.ArSceneView;
import com.opentok.android.BaseVideoCapturer;

public class CustomVideoCapturer extends BaseVideoCapturer implements PixelCopy.OnPixelCopyFinishedListener {
    private Bitmap mBitmap;
    private ArSceneView mContentView;
    private Canvas mCanvas;
    private Boolean surfaceCreated = false;

    private boolean capturing = false;
    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;

    private Handler mHandler = new Handler();
    private Handler handlerPixelCopy = new Handler();

    private Runnable newFrame = new Runnable() {
        @Override
        public void run() {
            if (capturing) {
                int width = mContentView.getWidth();
                int height = mContentView.getHeight();
                if (frame == null || CustomVideoCapturer.this.width != width || CustomVideoCapturer.this.height != height) {
                    CustomVideoCapturer.this.width = width;
                    CustomVideoCapturer.this.height = height;

                    if (mBitmap != null) {
                        mBitmap.recycle();
                        mBitmap = null;
                    }

                    mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    mBitmap.eraseColor(Color.BLUE);
                    mCanvas = new Canvas(mBitmap);
                    frame = new int[width * height];
                }

                // Save the pixels inside mContectView to mBitmap.
                PixelCopy.request(mContentView, mBitmap, CustomVideoCapturer.this, handlerPixelCopy);
            }
        }
    };

    public CustomVideoCapturer(ArSceneView view) {
        this.mContentView = view;
    }

    // BaseVideoCapturer methods
    @Override
    public void init() {

    }

    @Override
    public int startCapture() {
        capturing = true;
        mHandler.postDelayed(newFrame, 1000 / fps);
        return 0;
    }

    @Override
    public int stopCapture() {
        capturing = false;
        mHandler.removeCallbacks(newFrame);
        return 0;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isCaptureStarted() {
        return capturing;
    }

    @Override
    public CaptureSettings getCaptureSettings() {
        CaptureSettings captureSettings = new CaptureSettings();
        captureSettings.fps = fps;
        captureSettings.width = width;
        captureSettings.height = height;
        captureSettings.format = ARGB;
        return captureSettings;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    // PixelCopy.onPixelCopyFinishedListener method
    @Override
    public void onPixelCopyFinished(int copyResult) {
        mBitmap.getPixels(frame, 0, width, 0, 0, width, height);

        // this method will send the frame directly to stream
        provideIntArrayFrame(frame, ARGB, width, height, 0, false);
        mHandler.postDelayed(newFrame, 1000 / fps);
    }
}
