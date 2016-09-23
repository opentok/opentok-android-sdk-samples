package com.opentok.android.samples.custom_video_driver;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.opentok.android.BaseVideoCapturer;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CustomVideoCapturer extends BaseVideoCapturer implements
        PreviewCallback {

    private final static String LOGTAG = "customer-video-capturer";

    private int mCameraIndex = 0;
    private Camera mCamera;
    private Camera.CameraInfo mCurrentDeviceInfo = null;
    private ReentrantLock mPreviewBufferLock = new ReentrantLock(); // sync
    // start/stop
    // capture
    // and
    // surface
    // changes

    private final static int PIXEL_FORMAT = ImageFormat.NV21;
    private final static int PREFERRED_CAPTURE_WIDTH = 640;
    private final static int PREFERRED_CAPTURE_HEIGHT = 480;

    private boolean isCaptureStarted = false;
    private boolean isCaptureRunning = false;

    private final int mNumCaptureBuffers = 3;
    private int mExpectedFrameSize = 0;

    private int mCaptureWidth = -1;
    private int mCaptureHeight = -1;
    private int mCaptureFPS = -1;

    private Display mCurrentDisplay;
    private SurfaceTexture mSurfaceTexture;

    public CustomVideoCapturer(Context context) {

        // Initialize front camera by default
        this.mCameraIndex = getFrontCameraIndex();

        // Get current display to query UI orientation
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        mCurrentDisplay = windowManager.getDefaultDisplay();

    }

    @Override
    public int startCapture() {
        if (isCaptureStarted) {
            return -1;
        }

        // Set the preferred capturing size
        configureCaptureSize(PREFERRED_CAPTURE_WIDTH, PREFERRED_CAPTURE_HEIGHT);

        // Set the capture parameters
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mCaptureWidth, mCaptureHeight);
        parameters.setPreviewFormat(PIXEL_FORMAT);
        parameters.setPreviewFrameRate(mCaptureFPS);
        try {
            mCamera.setParameters(parameters);
        } catch (RuntimeException e) {
            Log.e(LOGTAG, "setParameters failed", e);
            return -1;
        }

        // Create capture buffers
        PixelFormat pixelFormat = new PixelFormat();
        PixelFormat.getPixelFormatInfo(PIXEL_FORMAT, pixelFormat);
        int bufSize = mCaptureWidth * mCaptureHeight * pixelFormat.bitsPerPixel
                / 8;
        byte[] buffer = null;
        for (int i = 0; i < mNumCaptureBuffers; i++) {
            buffer = new byte[bufSize];
            mCamera.addCallbackBuffer(buffer);
        }

        try {
            mSurfaceTexture = new SurfaceTexture(42);
            mCamera.setPreviewTexture(mSurfaceTexture);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Start preview
        mCamera.setPreviewCallbackWithBuffer(this);
        mCamera.startPreview();

        mPreviewBufferLock.lock();
        mExpectedFrameSize = bufSize;
        isCaptureRunning = true;
        mPreviewBufferLock.unlock();

        isCaptureStarted = true;

        return 0;
    }

    @Override
    public int stopCapture() {
        mPreviewBufferLock.lock();
        try {
            if (isCaptureRunning) {
                isCaptureRunning = false;
                mCamera.stopPreview();
                mCamera.setPreviewCallbackWithBuffer(null);
            }
        } catch (RuntimeException e) {
            Log.e(LOGTAG, "Failed to stop camera", e);
            return -1;
        }
        mPreviewBufferLock.unlock();

        isCaptureStarted = false;
        return 0;
    }

    @Override
    public void destroy() {
        if (mCamera == null) {
            return;
        }
        stopCapture();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public boolean isCaptureStarted() {
        return isCaptureStarted;
    }

    @Override
    public CaptureSettings getCaptureSettings() {

        // Set the preferred capturing size
        configureCaptureSize(PREFERRED_CAPTURE_WIDTH, PREFERRED_CAPTURE_HEIGHT);

        CaptureSettings settings = new CaptureSettings();
        settings.fps = mCaptureFPS;
        settings.width = mCaptureWidth;
        settings.height = mCaptureHeight;
        settings.format = NV21;
        settings.expectedDelay = 0;
        return settings;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    /*
     * Get the natural camera orientation
     */
    private int getNaturalCameraOrientation() {
        if (mCurrentDeviceInfo != null) {
            return mCurrentDeviceInfo.orientation;
        } else {
            return 0;
        }
    }

    /*
     * Check if the current camera is a front camera
     */
    public boolean isFrontCamera() {
      return (mCurrentDeviceInfo != null && mCurrentDeviceInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    /*
     * Returns the currently active camera ID.
     */
    public int getCameraIndex() {
        return mCameraIndex;
    }

    /*
     * Switching between cameras if there are multiple cameras on the device.
     */
    public void swapCamera(int index) {
        boolean wasStarted = this.isCaptureStarted;

        if (mCamera != null) {
            stopCapture();
            mCamera.release();
            mCamera = null;
        }

        this.mCameraIndex = index;
        this.mCamera = Camera.open(index);
        this.mCurrentDeviceInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(index, mCurrentDeviceInfo);

        if (wasStarted) {
            startCapture();
        }
    }

    /*
     * Set current camera orientation
     */
    private int compensateCameraRotation(int uiRotation) {

        int cameraRotation = 0;
        switch (uiRotation) {
            case (Surface.ROTATION_0):
                cameraRotation = 0;
                break;
            case (Surface.ROTATION_90):
                cameraRotation = 270;
                break;
            case (Surface.ROTATION_180):
                cameraRotation = 180;
                break;
            case (Surface.ROTATION_270):
                cameraRotation = 90;
                break;
            default:
                break;
        }

        int cameraOrientation = this.getNaturalCameraOrientation();

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

    /*
     * Set camera index
     */
    private static int getFrontCameraIndex() {
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return 0;
    }

    private void configureCaptureSize(int preferredWidth, int preferredHeight) {
        Camera.Parameters parameters = mCamera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        @SuppressWarnings("deprecation")
        List<Integer> frameRates = parameters.getSupportedPreviewFrameRates();
        int maxFPS = 0;
        if (frameRates != null) {
            for (Integer frameRate : frameRates) {
                if (frameRate > maxFPS) {
                    maxFPS = frameRate;
                }
            }
        }
        mCaptureFPS = maxFPS;

        int maxw = 0;
        int maxh = 0;
        for (int i = 0; i < sizes.size(); ++i) {
            Size s = sizes.get(i);
            if (s.width >= maxw && s.height >= maxh) {
                if (s.width <= preferredWidth && s.height <= preferredHeight) {
                    maxw = s.width;
                    maxh = s.height;
                }
            }
        }
        if (maxw == 0 || maxh == 0) {
            Size s = sizes.get(0);
            maxw = s.width;
            maxh = s.height;
        }

        mCaptureWidth = maxw;
        mCaptureHeight = maxh;
    }

    @Override
    public void init() {
        mCamera = Camera.open(mCameraIndex);
        mCurrentDeviceInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraIndex, mCurrentDeviceInfo);

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mPreviewBufferLock.lock();
        if (isCaptureRunning) {
            if (data.length == mExpectedFrameSize) {
                // Get the rotation of the camera
                int currentRotation = compensateCameraRotation(mCurrentDisplay
                        .getRotation());

                // Send frame to OpenTok
                provideByteArrayFrame(data, NV21, mCaptureWidth,
                        mCaptureHeight, currentRotation, isFrontCamera());

                // Reuse the video buffer
                camera.addCallbackBuffer(data);
            }
        }
        mPreviewBufferLock.unlock();
    }

}
