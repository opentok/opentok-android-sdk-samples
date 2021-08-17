package com.tokbox.sample.screensharing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.View;

import com.opentok.android.BaseVideoCapturer;

public class ScreenSharingCapturer extends BaseVideoCapturer {

  private Context context;

    private boolean capturing = false;
    private View contentView;

    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;

    private Bitmap bmp;
    private Canvas canvas;

    private Handler handler = new Handler();

    private Runnable newFrame = new Runnable() {
        @Override
        public void run() {
            if (capturing) {
                int width = contentView.getWidth();
                int height = contentView.getHeight();

                if (frame == null ||
                        ScreenSharingCapturer.this.width != width ||
                        ScreenSharingCapturer.this.height != height) {

                    ScreenSharingCapturer.this.width = width;
                    ScreenSharingCapturer.this.height = height;

                    if (bmp != null) {
                        bmp.recycle();
                        bmp = null;
                    }

                    bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                    canvas = new Canvas(bmp);
                    frame = new int[width * height];
                }
                canvas.saveLayer(0, 0, width, height, null);
                canvas.translate(-contentView.getScrollX(), - contentView.getScrollY());
                contentView.draw(canvas);

                bmp.getPixels(frame, 0, width, 0, 0, width, height);

                provideIntArrayFrame(frame, ARGB, width, height, 0, false);

                canvas.restore();

                handler.postDelayed(newFrame, 1000 / fps);

            }
        }
    };

    public ScreenSharingCapturer(Context context, View view) {
        this.context = context;
        this.contentView = view;
    }

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
    public void destroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

}