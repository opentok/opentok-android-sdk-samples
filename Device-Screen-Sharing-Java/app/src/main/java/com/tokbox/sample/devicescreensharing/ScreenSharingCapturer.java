package com.tokbox.sample.devicescreensharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import java.nio.ByteBuffer;

import com.opentok.android.BaseVideoCapturer;

public class ScreenSharingCapturer extends BaseVideoCapturer {

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private Context context;

    private boolean capturing = false;

    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;

    public ScreenSharingCapturer(Context context, MediaProjection mediaProjection) {
        this.context = context;
        this.mediaProjection = mediaProjection;
        initDisplayMetrics();
    }

    private void initDisplayMetrics() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            width = displayMetrics.widthPixels;
            height = displayMetrics.heightPixels;
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void init() {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        startBackgroundThread();
    }

    private void createVirtualDisplay() {

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                // MediaProjection was stopped, release resources
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
            }
        }, new Handler(Looper.getMainLooper()));

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenSharing",
            width, height, context.getResources().getDisplayMetrics().densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null,
            backgroundHandler
        );
        
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();

                    if (frame == null) {
                        frame = new int[width * height];
                    }

                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int index = y * rowStride + x * pixelStride;
                            int pixel = buffer.getInt(index);
                            frame[y * width + x] = pixel;
                        }
                    }

                    provideIntArrayFrame(frame, ABGR, width, height, 0, false);
                    image.close();
                }
            }
        }, backgroundHandler);
    }

    @Override
    public int startCapture() {
        capturing = true;
        return 0;
    }

    @Override
    public int stopCapture() {
        capturing = false;
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        stopBackgroundThread();
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
        captureSettings.format = ABGR;
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

    private void startBackgroundThread() {
        createVirtualDisplay();
        backgroundThread = new HandlerThread("ScreenCapture");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}