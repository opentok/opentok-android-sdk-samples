package com.example.tokbox.CustomVideoDriverLib;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.Publisher;

@TargetApi(21)
@RequiresApi(21)
public
class CustomVideoCapturerCamera2 extends BaseVideoCapturer implements BaseVideoCapturer.CaptureSwitch {
    private static final int PREFERRED_FACING_CAMERA = CameraMetadata.LENS_FACING_FRONT;
    private static final int PIXEL_FORMAT = ImageFormat.YUV_420_888;
    private static final String LOG_TAG = CustomVideoCapturerCamera2.class.getSimpleName();

    private enum CameraState {
        CLOSED,
        CLOSING,
        SETUP,
        OPEN,
        CAPTURE,
        ERROR
    }

    private CameraManager cameraManager;
    private CameraDevice camera;
    private HandlerThread cameraThread;
    private Handler cameraThreadHandler;
    private ImageReader cameraFrame;
    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private CameraInfoCache characteristics;
    private CameraState cameraState;
    private Display display;
    private DisplayOrientationCache displayOrientationCache;
    private ReentrantLock reentrantLock;
    private Condition condition;
    private int cameraIndex;
    private Size frameDimensions;
    private int desiredFps;
    private Range<Integer> camFps;
    private List<RuntimeException> runtimeExceptionList;
    private boolean isPaused;

    private Runnable executeAfterClosed;
    private Runnable executeAfterCameraOpened;

    private static final SparseIntArray rotationTable = new SparseIntArray() {
        {
            append(Surface.ROTATION_0, 0);
            append(Surface.ROTATION_90, 90);
            append(Surface.ROTATION_180, 180);
            append(Surface.ROTATION_270, 270);
        }
    };
    private static final SparseArray<Size> resolutionTable = new SparseArray<Size>() {
        {
            append(Publisher.CameraCaptureResolution.LOW.ordinal(), new Size(352, 288));
            append(Publisher.CameraCaptureResolution.MEDIUM.ordinal(), new Size(640, 480));
            append(Publisher.CameraCaptureResolution.HIGH.ordinal(), new Size(1280, 720));
        }
    };
    private static final SparseIntArray frameRateTable = new SparseIntArray() {
        {
            append(Publisher.CameraCaptureFrameRate.FPS_1.ordinal(), 1);
            append(Publisher.CameraCaptureFrameRate.FPS_7.ordinal(), 7);
            append(Publisher.CameraCaptureFrameRate.FPS_15.ordinal(), 15);
            append(Publisher.CameraCaptureFrameRate.FPS_30.ordinal(), 30);
        }
    };

    /* Observers/Notification callback objects */
    private CameraDevice.StateCallback cameraObserver = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(LOG_TAG,"CameraDevice onOpened");
            cameraState = CameraState.OPEN;
            CustomVideoCapturerCamera2.this.camera = camera;
            if (executeAfterCameraOpened != null) {
                executeAfterCameraOpened.run();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            try {
                Log.d(LOG_TAG,"CameraDevice onDisconnected");
                CustomVideoCapturerCamera2.this.camera.close();
            } catch (NullPointerException e) {
                // does nothing
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            try {
                Log.d(LOG_TAG,"CameraDevice onError");
                CustomVideoCapturerCamera2.this.camera.close();
                // wait for condition variable
            } catch (NullPointerException e) {
                // does nothing
            }
            postAsyncException(new Camera2Exception("Camera Open Error: " + error));
        }

        @Override
        public void onClosed(CameraDevice camera) {
            Log.d(LOG_TAG,"CameraDevice onClosed");
            super.onClosed(camera);
            cameraState = CameraState.CLOSED;
            CustomVideoCapturerCamera2.this.camera = null;

            if (executeAfterClosed != null) {
                executeAfterClosed.run();
            }

        }
    };

    private ImageReader.OnImageAvailableListener frameObserver = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image frame = reader.acquireNextImage();
            if (frame == null
                    || (frame.getPlanes().length > 0 && frame.getPlanes()[0].getBuffer() == null)
                    || (frame.getPlanes().length > 1 && frame.getPlanes()[1].getBuffer() == null)
                    || (frame.getPlanes().length > 2 && frame.getPlanes()[2].getBuffer() == null))
            {
                Log.d(LOG_TAG,"onImageAvailable frame provided has no image data");
                return;
            }

            if (CameraState.CAPTURE == cameraState) {
                provideBufferFramePlanar(
                        frame.getPlanes()[0].getBuffer(),
                        frame.getPlanes()[1].getBuffer(),
                        frame.getPlanes()[2].getBuffer(),
                        frame.getPlanes()[0].getPixelStride(),
                        frame.getPlanes()[0].getRowStride(),
                        frame.getPlanes()[1].getPixelStride(),
                        frame.getPlanes()[1].getRowStride(),
                        frame.getPlanes()[2].getPixelStride(),
                        frame.getPlanes()[2].getRowStride(),
                        frame.getWidth(),
                        frame.getHeight(),
                        calculateCamRotation(),
                        isFrontCamera()
                );
            }
            frame.close();
        }
    };

    private CameraCaptureSession.StateCallback captureSessionObserver =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(LOG_TAG,"CaptureSession onConfigured");
                    try {
                        cameraState = CameraState.CAPTURE;
                        captureSession = session;
                        captureRequest = captureRequestBuilder.build();
                        captureSession.setRepeatingRequest(captureRequest, captureNotification, cameraThreadHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d(LOG_TAG,"CaptureSession onFailed");
                    cameraState = CameraState.ERROR;
                    postAsyncException(new Camera2Exception("Camera session configuration failed"));
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    Log.d(LOG_TAG,"CaptureSession onClosed");
                    if (camera != null) {
                        camera.close();
                    }
                }
            };

    private CameraCaptureSession.CaptureCallback captureNotification =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
            };


    /* caching of camera characteristics & display orientation for performance */
    private static class CameraInfoCache {
        private CameraCharacteristics   info;
        private boolean                 frontFacing = false;
        private int                     sensorOrientation = 0;

        public CameraInfoCache(CameraCharacteristics info) {
            info    = info;
            /* its actually faster to cache these results then to always look
               them up, and since they are queried every frame...
             */
            frontFacing = info.get(CameraCharacteristics.LENS_FACING)
                    == CameraCharacteristics.LENS_FACING_FRONT;
            sensorOrientation = info.get(CameraCharacteristics.SENSOR_ORIENTATION).intValue();
        }

        public <T> T get(CameraCharacteristics.Key<T> key) {
            return info.get(key);
        }

        public boolean isFrontFacing() {
            return frontFacing;
        }

        public int sensorOrientation() {
            return sensorOrientation;
        }
    }

    private static class DisplayOrientationCache implements Runnable {
        private static final int    POLL_DELAY_MS = 750; /* 750 ms */
        private int displayRotation;
        private Display display;
        private Handler handler;

        public DisplayOrientationCache(Display dsp, Handler hndlr) {
            display = dsp;
            handler = hndlr;
            displayRotation = rotationTable.get(display.getRotation());
            handler.postDelayed(this, POLL_DELAY_MS);
        }

        public int getOrientation() {
            return displayRotation;
        }

        @Override
        public void run() {
            displayRotation = rotationTable.get(display.getRotation());
            handler.postDelayed(this, POLL_DELAY_MS);
        }
    }

    /* custom exceptions */
    public static class Camera2Exception extends RuntimeException {
        public Camera2Exception(String message) {
            super(message);
        }
    }

    /* Constructors etc... */
    public CustomVideoCapturerCamera2(Context ctx,
                                Publisher.CameraCaptureResolution resolution,
                                Publisher.CameraCaptureFrameRate fps) {
        cameraManager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        camera = null;
        cameraState = CameraState.CLOSED;
        reentrantLock = new ReentrantLock();
        condition = reentrantLock.newCondition();
        frameDimensions = resolutionTable.get(resolution.ordinal());
        desiredFps = frameRateTable.get(fps.ordinal());
        runtimeExceptionList = new ArrayList<RuntimeException>();
        isPaused = false;
        try {
            String camId = selectCamera(PREFERRED_FACING_CAMERA);
            /* if default camera facing direction is not found, use first camera */
            if (null == camId && (0 < cameraManager.getCameraIdList().length)) {
                camId = cameraManager.getCameraIdList()[0];
            }
            cameraIndex = findCameraIndex(camId);
        } catch (CameraAccessException e) {
            throw new Camera2Exception(e.getMessage());
        }
    }

    /**
     * Initializes the video capturer.
     */
    @Override
    public synchronized void init() {
        Log.d(LOG_TAG,"init enter");
        characteristics = null;
        // start camera looper thread
        startCamThread();
        // start display orientation polling
        startDisplayOrientationCache();
        // open selected camera
        initCamera();
        Log.d(LOG_TAG,"init exit");
    }

    private int doStartCapture() {
        Log.d(LOG_TAG,"doStartCapture enter");
        try {
            // create camera preview request
            if (isFrontCamera()) {
                captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(cameraFrame.getSurface());
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, camFps);
                captureRequestBuilder.set(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_USE_SCENE_MODE
                );
                captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                );
                captureRequestBuilder.set(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY
                );
                camera.createCaptureSession(
                        Arrays.asList(cameraFrame.getSurface()),
                        captureSessionObserver,
                        cameraThreadHandler
                );
            } else {
                captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                captureRequestBuilder.addTarget(cameraFrame.getSurface());
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, camFps);
                captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                );
                camera.createCaptureSession(
                        Arrays.asList(cameraFrame.getSurface()),
                        captureSessionObserver,
                        cameraThreadHandler
                );
            }
        } catch (CameraAccessException e) {
            throw new Camera2Exception(e.getMessage());
        }
        Log.d(LOG_TAG,"doStartCapture exit");
        return 0;
    }

    /**
     * Starts capturing video.
     */
    @Override
    public synchronized int startCapture() {
        Log.d(LOG_TAG,"startCapture enter (cameraState: "+ cameraState +")");
        if (null != camera && CameraState.OPEN == cameraState) {
            return doStartCapture();
        } else if (CameraState.SETUP == cameraState) {
            Log.d(LOG_TAG,"camera not yet ready, queuing the start until camera is opened.");
            executeAfterCameraOpened = () -> doStartCapture();
        } else {
            throw new Camera2Exception("Start Capture called before init successfully completed.");
        }
        Log.d(LOG_TAG,"startCapture exit");
        return 0;
    }

    /**
     * Stops capturing video.
     */
    @Override
    public synchronized int stopCapture() {
        Log.d(LOG_TAG,"stopCapture enter");
        if (null != camera && null != captureSession && CameraState.CLOSED != cameraState) {
            cameraState = CameraState.CLOSING;
            try {
                captureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            captureSession.close();
            cameraFrame.close();
            characteristics = null;
        }
        Log.d(LOG_TAG,"stopCapture exit");
        return 0;
    }

    /**
     * Destroys the BaseVideoCapturer object.
     */
    @Override
    public synchronized void destroy() {
        Log.d(LOG_TAG,"destroy enter");
        /* stop display orientation polling */
        stopDisplayOrientationCache();
        /* stop camera message thread */
        stopCamThread();
        Log.d(LOG_TAG,"destroy exit");
    }

    /**
     * Whether video is being captured (true) or not (false).
     */
    @Override
    public boolean isCaptureStarted() {
        return (cameraState == CameraState.CAPTURE);
    }

    /**
     * Returns the settings for the video capturer.
     */
    @Override
    public synchronized CaptureSettings getCaptureSettings() {
        //log.d("getCaptureSettings enter");
        CaptureSettings retObj = new CaptureSettings();
        retObj.fps = desiredFps;
        retObj.width = (null != cameraFrame) ? cameraFrame.getWidth() : 0;
        retObj.height = (null != cameraFrame) ? cameraFrame.getHeight() : 0;
        retObj.format = BaseVideoCapturer.NV21;
        retObj.expectedDelay = 0;
        //retObj.mirrorInLocalRender = frameMirrorX;
        //log.d("getCaptureSettings exit");
        return retObj;
    }

    /**
     * Call this method when the activity pauses. When you override this method, implement code
     * to respond to the activity being paused. For example, you may pause capturing audio or video.
     *
     * @see #onResume()
     */
    @Override
    public synchronized void onPause() {
        Log.d(LOG_TAG,"onPause");
        /* shutdown old camera but not the camera-callback thread */
        switch (cameraState) {
            case CAPTURE:
                stopCapture();
                isPaused = true;
                break;
            case SETUP:
            default:
                break;
        }
    }

    /**
     * Call this method when the activity resumes. When you override this method, implement code
     * to respond to the activity being resumed. For example, you may resume capturing audio
     * or video.
     *
     * @see #onPause()
     */
    @Override
    public void onResume() {
        Log.d(LOG_TAG,"onResume");
        if (isPaused) {
            Runnable resume = () -> {
                initCamera();
                startCapture();
            };
            if (cameraState == CameraState.CLOSING) {
                executeAfterClosed = resume;
            } else if (cameraState == CameraState.CLOSED){
                resume.run();
            }
            isPaused = false;
        } else {
            Log.d(LOG_TAG,"Capturer was not paused when onResume was called");
        }
    }

    @Override
    public synchronized void cycleCamera() {
        try {
            String[] camLst = cameraManager.getCameraIdList();
            swapCamera((cameraIndex + 1) % camLst.length);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new Camera2Exception(e.getMessage());
        }
    }

    @Override
    public int getCameraIndex() {
        return cameraIndex;
    }

    @Override
    public synchronized void swapCamera(int cameraId) {
        CameraState oldState = cameraState;
        /* shutdown old camera but not the camera-callback thread */
        switch (oldState) {
            case CAPTURE:
                stopCapture();
                break;
            case SETUP:
            default:
                break;
        }
        /* set camera ID */
        cameraIndex = cameraId;
        executeAfterClosed = () -> {
            switch (oldState) {
                case CAPTURE:
                    initCamera();
                    startCapture();
                    break;
                case SETUP:
                default:
                    break;
            }
        };

    }

    private boolean isFrontCamera() {
        return (characteristics != null) && characteristics.isFrontFacing();
    }

    private void startCamThread() {
        cameraThread = new HandlerThread("Camera2VideoCapturer-Camera-Thread");
        cameraThread.start();
        cameraThreadHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCamThread() {
        try {
            cameraThread.quitSafely();
            cameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // does nothing
        } finally {
            cameraThread = null;
            cameraThreadHandler = null;
        }
    }

    private String selectCamera(int lenseDirection) throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics info = cameraManager.getCameraCharacteristics(id);
            /* discard cameras that don't face the right direction */
            if (lenseDirection == info.get(CameraCharacteristics.LENS_FACING)) {
                Log.d(LOG_TAG,"selectCamera() Direction the camera faces relative to device screen: " + info.get(CameraCharacteristics.LENS_FACING));
                return id;
            }
        }
        return null;
    }

    private Range<Integer> selectCameraFpsRange(String camId, final int fps) throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            if (id.equals(camId)) {
                CameraCharacteristics info = cameraManager.getCameraCharacteristics(id);
                List<Range<Integer>> fpsLst = new ArrayList<>();
                Collections.addAll(fpsLst,
                        info.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES));
                /* sort list by error from desired fps *
                 * Android seems to do a better job at color correction/avoid 'dark frames' issue by
                 * selecting camera settings with the smallest lower bound on allowed frame rate
                 * range. */
                return Collections.min(fpsLst, new Comparator<Range<Integer>>() {
                    @Override
                    public int compare(Range<Integer> lhs, Range<Integer> rhs) {
                        return calcError(lhs) - calcError(rhs);
                    }

                    private int calcError(Range<Integer> val) {
                        return val.getLower() + Math.abs(val.getUpper() - fps);
                    }
                });
            }
        }
        return null;
    }

    private int findCameraIndex(String camId) throws CameraAccessException {
        String[] idList = cameraManager.getCameraIdList();
        for (int ndx = 0; ndx < idList.length; ++ndx) {
            if (idList[ndx].equals(camId)) {
                return ndx;
            }
        }
        return -1;
    }

    private Size selectPreferredSize(String camId, final int width, final int height, int format)
            throws CameraAccessException {
        CameraCharacteristics info = cameraManager.getCameraCharacteristics(camId);
        StreamConfigurationMap dimMap = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        List<Size> sizeLst = new ArrayList<Size>();
        int[] formats = dimMap.getOutputFormats();
        Collections.addAll(sizeLst, dimMap.getOutputSizes(ImageFormat.YUV_420_888));
        /* sort list by error from desired size */
        return Collections.min(sizeLst, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                int lXerror = Math.abs(lhs.getWidth() - width);
                int lYerror = Math.abs(lhs.getHeight() - height);
                int rXerror = Math.abs(rhs.getWidth() - width);
                int rYerror = Math.abs(rhs.getHeight() - height);
                return (lXerror + lYerror) - (rXerror + rYerror);
            }
        });
    }

    /*
     * Set current camera orientation
     */
    private int calculateCamRotation() {
        if (characteristics != null) {
            int cameraRotation = displayOrientationCache.getOrientation();
            int cameraOrientation = characteristics.sensorOrientation();
            if (!characteristics.isFrontFacing()) {
                return Math.abs((cameraRotation - cameraOrientation) % 360);
            } else {
                return (cameraRotation + cameraOrientation + 360) % 360;
            }
        } else {
            return 0;
        }
    }

    @SuppressLint("all")
    private void initCamera() {
        Log.d(LOG_TAG,"initCamera()");
        try {
            cameraState = CameraState.SETUP;
            // find desired camera & camera ouput size
            String[] cameraIdList = cameraManager.getCameraIdList();
            String camId = cameraIdList[cameraIndex];
            camFps = selectCameraFpsRange(camId, desiredFps);
            Size preferredSize = selectPreferredSize(
                    camId,
                    frameDimensions.getWidth(),
                    frameDimensions.getHeight(),
                    PIXEL_FORMAT
            );
            cameraFrame = ImageReader.newInstance(
                    preferredSize.getWidth(),
                    preferredSize.getHeight(),
                    PIXEL_FORMAT,
                    3
            );
            cameraFrame.setOnImageAvailableListener(frameObserver, cameraThreadHandler);
            characteristics = new CameraInfoCache(cameraManager.getCameraCharacteristics(camId));
            cameraManager.openCamera(camId, cameraObserver, cameraThreadHandler);
        } catch (CameraAccessException exp) {
            throw new Camera2Exception(exp.getMessage());
        }
    }

    private void postAsyncException(RuntimeException exp) {
        runtimeExceptionList.add(exp);
    }

    private void startDisplayOrientationCache() {
        displayOrientationCache = new DisplayOrientationCache(display, cameraThreadHandler);
    }

    private void stopDisplayOrientationCache() {
        cameraThreadHandler.removeCallbacks(displayOrientationCache);
    }
}