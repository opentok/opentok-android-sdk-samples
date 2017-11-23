package com.tokbox.android.tutorials.custom_video_driver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.Publisher;
import com.opentok.android.VideoUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


public class CustomVideoCapturer extends BaseVideoCapturer implements
        PreviewCallback,
        BaseVideoCapturer.CaptureSwitch {

    private static final String LOG_TAG = CustomVideoCapturer.class.getSimpleName();
    private int cameraIndex = 0;
    private Camera camera;
    private Camera.CameraInfo currentDeviceInfo = null;
    public ReentrantLock previewBufferLock = new ReentrantLock();

    private static final int PIXEL_FORMAT = ImageFormat.NV21;
    PixelFormat pixelFormat = new PixelFormat();

    // True when the C++ layer has ordered the camera to be started.
    private boolean isCaptureStarted = false;
    private boolean isCaptureRunning = false;

    private final int numCaptureBuffers = 3;
    private int expectedFrameSize = 0;

    private int captureWidth = -1;
    private int captureHeight = -1;
    private int[] captureFpsRange;

    private Display currentDisplay;

    private SurfaceTexture surfaceTexture;

    private Publisher publisher;
    private boolean blackFrames = false;
    private boolean isCapturePaused = false;

    private Publisher.CameraCaptureResolution preferredResolution =
            Publisher.CameraCaptureResolution.MEDIUM;
    private Publisher.CameraCaptureFrameRate preferredFramerate =
            Publisher.CameraCaptureFrameRate.FPS_30;

    //default case
    int fps = 1;
    int width = 0;
    int height = 0;
    int[] frame;
    Handler handler = new Handler();

    Runnable newFrame = new Runnable() {
        @Override
        public void run() {
            if (isCaptureRunning) {
                if (frame == null) {
                    VideoUtils.Size resolution = new VideoUtils.Size();
                    resolution = getPreferredResolution();
                    fps = getPreferredFramerate();
                    width = resolution.width;
                    height = resolution.height;
                    frame = new int[width * height];
                }

                provideIntArrayFrame(frame, ARGB, width, height, 0, false);
                handler.postDelayed(newFrame, 1000 / fps);
            }
        }
    };

    public CustomVideoCapturer(Context context, Publisher.CameraCaptureResolution resolution,
                                Publisher.CameraCaptureFrameRate fps) {
        this.cameraIndex = getCameraIndexUsingFront(true);

        // Get current display to query UI orientation
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        currentDisplay = windowManager.getDefaultDisplay();
        this.preferredFramerate = fps;
        this.preferredResolution = resolution;
    }

    public synchronized void init() {
        Log.d(LOG_TAG, "init() enetered");
        try {
            camera = Camera.open(cameraIndex);
        } catch (RuntimeException exp) {
            Log.e(LOG_TAG, "The camera is in use by another app");
        }

        currentDeviceInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraIndex, currentDeviceInfo);
        Log.d(LOG_TAG, "init() exit");
    }

    @Override
    public synchronized int startCapture() {
        Log.d(LOG_TAG, "started() entered");
        if (isCaptureStarted) {
            return -1;
        }

        if (camera != null) {
            //check preferredResolution and preferredFramerate values
            VideoUtils.Size resolution = getPreferredResolution();
            configureCaptureSize(resolution.width, resolution.height);

            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(captureWidth, captureHeight);
            parameters.setPreviewFormat(PIXEL_FORMAT);
            parameters.setPreviewFpsRange(captureFpsRange[0], captureFpsRange[1]);

            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            try {
                camera.setParameters(parameters);
            } catch (RuntimeException exp) {
                Log.e(LOG_TAG, "Camera.setParameters(parameters) - failed");
                return -1;
            }

            // Create capture buffers
            PixelFormat.getPixelFormatInfo(PIXEL_FORMAT, pixelFormat);
            int bufSize = captureWidth * captureHeight * pixelFormat.bitsPerPixel
                    / 8;
            byte[] buffer = null;
            for (int i = 0; i < numCaptureBuffers; i++) {
                buffer = new byte[bufSize];
                camera.addCallbackBuffer(buffer);
            }

            try {
                surfaceTexture = new SurfaceTexture(42);
                camera.setPreviewTexture(surfaceTexture);

            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();

            previewBufferLock.lock();
            expectedFrameSize = bufSize;

            previewBufferLock.unlock();

        } else {
            blackFrames = true;
            handler.postDelayed(newFrame, 1000 / fps);
        }

        isCaptureRunning = true;
        isCaptureStarted = true;
        Log.d(LOG_TAG, "started() exit");
        return 0;
    }

    @Override
    public synchronized int stopCapture() {
        if (camera != null) {
            previewBufferLock.lock();
            try {
                if (isCaptureRunning) {
                    isCaptureRunning = false;
                    camera.stopPreview();
                    camera.setPreviewCallbackWithBuffer(null);
                    camera.release();
                    Log.d(LOG_TAG,"Camera capture is stopped");
                }
            } catch (RuntimeException exp) {
                Log.e(LOG_TAG, "Camera.stopPreview() - failed ");
                return -1;
            }
            previewBufferLock.unlock();
        }
        isCaptureStarted = false;

        if (blackFrames) {
            handler.removeCallbacks(newFrame);
        }

        return 0;
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    @Override
    public CaptureSettings getCaptureSettings() {

        CaptureSettings settings = new CaptureSettings();
        ;
        VideoUtils.Size resolution = new VideoUtils.Size();
        resolution = getPreferredResolution();

        int framerate = getPreferredFramerate();

        if (camera != null) {
            settings = new CaptureSettings();
            configureCaptureSize(resolution.width, resolution.height);
            settings.fps = framerate;
            settings.width = captureWidth;
            settings.height = captureHeight;
            settings.format = NV21;
            settings.expectedDelay = 0;
        } else {
            settings.fps = framerate;
            settings.width = resolution.width;
            settings.height = resolution.height;
            settings.format = ARGB;
        }

        return settings;
    }

    @Override
    public synchronized  void onPause() {
        if (isCaptureStarted) {
            isCapturePaused = true;
            stopCapture();
        }
    }

    @Override
    public void onResume() {
        if (isCapturePaused) {
            init();
            startCapture();
            isCapturePaused = false;
        }
    }

    private int getNaturalCameraOrientation() {
        if (currentDeviceInfo != null) {
            return currentDeviceInfo.orientation;
        } else {
            return 0;
        }
    }

    public boolean isFrontCamera() {
        if (currentDeviceInfo != null) {
            return currentDeviceInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        return false;
    }

    public int getCameraIndex() {
        return cameraIndex;
    }

    @Override
    public synchronized void cycleCamera() {
        swapCamera((getCameraIndex() + 1) % Camera.getNumberOfCameras());
    }

    @SuppressLint("DefaultLocale")
    public synchronized void swapCamera(int index) {
        boolean wasStarted = this.isCaptureStarted;
        if (camera != null) {
            stopCapture();
            camera.release();
            camera = null;
        }

        this.cameraIndex = index;

        if (wasStarted) {
            if (-1 != Build.MODEL.toLowerCase().indexOf("samsung")) {
                /* This was added to workaround a bug on some Samsung devices (OPENTOK-25126)
                 * but it introduces a bug on the Nexus 7 & 9 (OPENTOK-29246) so.... */
                forceCameraRelease(index);
            }
            this.camera = Camera.open(index);
            this.currentDeviceInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, currentDeviceInfo);

            startCapture();
        }
    }

    private int compensateCameraRotation(int uiRotation) {

        int currentDeviceOrientation = 0;
        switch (uiRotation) {
            case (Surface.ROTATION_0):
                currentDeviceOrientation = 0;
                break;
            case (Surface.ROTATION_90):
                currentDeviceOrientation = 270;
                break;
            case (Surface.ROTATION_180):
                currentDeviceOrientation = 180;
                break;
            case (Surface.ROTATION_270):
                currentDeviceOrientation = 90;
                break;
            default:
                break;
        }

        int cameraOrientation = this.getNaturalCameraOrientation();
        // The device orientation is the device's rotation relative to its
        // natural position.
        int cameraRotation = roundRotation(currentDeviceOrientation);

        int totalCameraRotation = 0;
        boolean usingFrontCamera = this.isFrontCamera();
        if (usingFrontCamera) {
            // The front camera rotates in the opposite direction of the
            // device.
            int inverseCameraRotation = (360 - cameraRotation) % 360;
            totalCameraRotation = (inverseCameraRotation + cameraOrientation) % 360;
        } else {
            totalCameraRotation = (cameraRotation + cameraOrientation) % 360;
        }
        return totalCameraRotation;
    }

    private static int roundRotation(int rotation) {
        return (int) (Math.round((double) rotation / 90) * 90) % 360;
    }

    private static int getCameraIndexUsingFront(boolean front) {
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (front && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            } else if (!front
                    && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        previewBufferLock.lock();

        if (isCaptureRunning) {
            // If StartCapture has been called but not StopCapture
            // Call the C++ layer with the captured frame
            if (data.length == expectedFrameSize) {

                int currentRotation = compensateCameraRotation(currentDisplay
                        .getRotation());
                // Send buffer
                provideByteArrayFrame(data, NV21, captureWidth,
                        captureHeight, currentRotation, isFrontCamera());

                // Give the video buffer to the camera service again.
                camera.addCallbackBuffer(data);
            }
        }
        previewBufferLock.unlock();
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    private boolean forceCameraRelease(int cameraIndex) {
        Camera camera = null;
        try {
            camera = Camera.open(cameraIndex);
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
        return false;
    }

    private VideoUtils.Size getPreferredResolution() {

        VideoUtils.Size resolution = new VideoUtils.Size();

        switch (this.preferredResolution) {
            case LOW:
                resolution.width = 352;
                resolution.height = 288;
                break;
            case MEDIUM:
                resolution.width = 640;
                resolution.height = 480;
                break;
            case HIGH:
                resolution.width = 1280;
                resolution.height = 720;
                break;
            default:
                break;
        }

        return resolution;
    }

    private int getPreferredFramerate() {

        int framerate = 0;

        switch (this.preferredFramerate) {
            case FPS_30:
                framerate = 30;
                break;
            case FPS_15:
                framerate = 15;
                break;
            case FPS_7:
                framerate = 7;
                break;
            case FPS_1:
                framerate = 1;
                break;
            default:
                break;
        }

        return framerate;
    }

    private void configureCaptureSize(int preferredWidth, int preferredHeight) {
        List<Size> sizes = null;
        int preferredFramerate = getPreferredFramerate();
        try {
            Camera.Parameters parameters = camera.getParameters();
            sizes = parameters.getSupportedPreviewSizes();
            captureFpsRange = findClosestEnclosingFpsRange(preferredFramerate * 1000,
                    parameters.getSupportedPreviewFpsRange());

        } catch (RuntimeException exp) {
            Log.e(LOG_TAG, "Error configuring capture size");
        }

        int maxw = 0;
        int maxh = 0;
        for (int i = 0; i < sizes.size(); ++i) {
            Size size = sizes.get(i);
            if (size.width >= maxw && size.height >= maxh) {
                if (size.width <= preferredWidth && size.height <= preferredHeight) {
                    maxw = size.width;
                    maxh = size.height;
                }
            }
        }

        if (maxw == 0 || maxh == 0) {
            // Not found a smaller resolution close to the preferred
            // So choose the lowest resolution possible
            Size size = sizes.get(0);
            int minw = size.width;
            int minh = size.height;
            for (int i = 1; i < sizes.size(); ++i) {
                size = sizes.get(i);
                if (size.width <= minw && size.height <= minh) {
                    minw = size.width;
                    minh = size.height;
                }
            }
            maxw = minw;
            maxh = minh;
        }

        captureWidth = maxw;
        captureHeight = maxh;
    }

    private int[] findClosestEnclosingFpsRange(int preferredFps, List<int[]> supportedFpsRanges) {
        if (supportedFpsRanges == null || supportedFpsRanges.size() == 0) {
            return new int[]{0, 0};
        }
        /* Because some versions of the Samsung S5 have luminescence issues with 30fps front
           faced cameras, lock to 24 */
        if (isFrontCamera()
                && "samsung-sm-g900a".equals(Build.MODEL.toLowerCase())
                && 30000 == preferredFps) {
            preferredFps = 24000;
        }

        final int fps = preferredFps;
        int[] closestRange = Collections.min(supportedFpsRanges, new Comparator<int[]>() {
            // Progressive penalty if the upper bound is further away than |MAX_FPS_DIFF_THRESHOLD|
            // from requested.
            private static final int MAX_FPS_DIFF_THRESHOLD = 5000;
            private static final int MAX_FPS_LOW_DIFF_WEIGHT = 1;
            private static final int MAX_FPS_HIGH_DIFF_WEIGHT = 3;
            // Progressive penalty if the lower bound is bigger than |MIN_FPS_THRESHOLD|.
            private static final int MIN_FPS_THRESHOLD = 8000;
            private static final int MIN_FPS_LOW_VALUE_WEIGHT = 1;
            private static final int MIN_FPS_HIGH_VALUE_WEIGHT = 4;
            // Use one weight for small |value| less than |threshold|, and another weight above.
            private int progressivePenalty(int value, int threshold, int lowWeight, int highWeight) {
                return (value < threshold)
                        ? value * lowWeight
                        : threshold * lowWeight + (value - threshold) * highWeight;
            }

            private int diff(int[] range) {
                final int minFpsError = progressivePenalty(range[0],
                        MIN_FPS_THRESHOLD, MIN_FPS_LOW_VALUE_WEIGHT, MIN_FPS_HIGH_VALUE_WEIGHT);
                final int maxFpsError = progressivePenalty(Math.abs(fps * 1000 - range[1]),
                        MAX_FPS_DIFF_THRESHOLD, MAX_FPS_LOW_DIFF_WEIGHT, MAX_FPS_HIGH_DIFF_WEIGHT);;
                return minFpsError + maxFpsError;
            }

            @Override
            public int compare(int[] lhs, int[] rhs) {
                return diff(lhs) - diff(rhs);
            }
        });

        checkRangeWithWarning(preferredFps, closestRange);
        return closestRange;
    }

    private void checkRangeWithWarning(int preferredFps, int[] range) {
        if (preferredFps < range[0] || preferredFps > range[1]) {
            Log.w(LOG_TAG,"Closest fps range found: "+ (range[0] / 1000) + (range[1] / 1000) + "for desired fps: "+ (preferredFps / 1000));
        }
    }
}
