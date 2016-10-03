package com.opentok.android.samples.screen_sharing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.View;

import com.opentok.android.BaseVideoCapturer;

public class ScreensharingCapturer extends BaseVideoCapturer {

  private Context mContext;

    private boolean capturing = false;
    private View contentView;

    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;

    private Bitmap bmp;
    private Canvas canvas;

    private Handler mHandler = new Handler();

    private Runnable newFrame = new Runnable() {
        @Override
        public void run() {
            if (capturing) {
                int width = contentView.getWidth();
                int height = contentView.getHeight();

                if (frame == null ||
                        ScreensharingCapturer.this.width != width ||
                        ScreensharingCapturer.this.height != height) {

                    ScreensharingCapturer.this.width = width;
                    ScreensharingCapturer.this.height = height;

                    if (bmp != null) {
                        bmp.recycle();
                        bmp = null;
                    }

                    bmp = Bitmap.createBitmap(width,
                            height, Bitmap.Config.ARGB_8888);

                    canvas = new Canvas(bmp);
                    frame = new int[width * height];
                }
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(-contentView.getScrollX(), - contentView.getScrollY());
                contentView.draw(canvas);

                bmp.getPixels(frame, 0, width, 0, 0, width, height);

                provideIntArrayFrame(frame, ARGB, width, height, 0, false);

                canvas.restore();

                mHandler.postDelayed(newFrame, 1000 / fps);

            }
        }
    };

    public ScreensharingCapturer(Context context, View view) {
        this.mContext = context;
        this.contentView = view;
    }

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
    public boolean isCaptureStarted() {
        return capturing;
    }

    @Override
    public CaptureSettings getCaptureSettings() {

        CaptureSettings settings = new CaptureSettings();
        settings.fps = fps;
        settings.width = width;
        settings.height = height;
        settings.format = ARGB;
        return settings;
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