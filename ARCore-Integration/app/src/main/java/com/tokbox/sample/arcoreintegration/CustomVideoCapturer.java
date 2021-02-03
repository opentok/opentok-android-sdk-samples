package com.tokbox.sample.arcoreintegration;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.view.PixelCopy;
import com.google.ar.sceneform.ArSceneView;
import com.opentok.android.BaseVideoCapturer;

public class CustomVideoCapturer extends BaseVideoCapturer implements PixelCopy.OnPixelCopyFinishedListener {
    private Bitmap bitmap;
    private ArSceneView contentView;
    private Canvas canvas;
    private Boolean surfaceCreated = false;

    private boolean capturing = false;
    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;

    private Handler handler = new Handler();
    private Handler handlerPixelCopy = new Handler();

    private Runnable newFrame = new Runnable() {
        @Override
        public void run() {
            if (capturing) {
                int width = contentView.getWidth();
                int height = contentView.getHeight();
                if (frame == null || CustomVideoCapturer.this.width != width || CustomVideoCapturer.this.height != height) {
                    CustomVideoCapturer.this.width = width;
                    CustomVideoCapturer.this.height = height;

                    if (bitmap != null) {
                        bitmap.recycle();
                        bitmap = null;
                    }

                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.BLUE);
                    canvas = new Canvas(bitmap);
                    frame = new int[width * height];
                }

                // Save the pixels inside contectView to mBitmap
                PixelCopy.request(contentView, bitmap, CustomVideoCapturer.this, handlerPixelCopy);
            }
        }
    };

    public CustomVideoCapturer(ArSceneView view) {
        this.contentView = view;
    }

    // BaseVideoCapturer methods
    @Override
    public void init() {

    }

    @Override
    public int startCapture() {
        capturing = true;
        handler.postDelayed(newFrame, 1000 / fps);
        return 0;
    }

    @Override
    public int stopCapture() {
        capturing = false;
        handler.removeCallbacks(newFrame);
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
        bitmap.getPixels(frame, 0, width, 0, 0, width, height);

        // this method will send the frame directly to stream
        provideIntArrayFrame(frame, ARGB, width, height, 0, false);
        handler.postDelayed(newFrame, 1000 / fps);
    }
}
