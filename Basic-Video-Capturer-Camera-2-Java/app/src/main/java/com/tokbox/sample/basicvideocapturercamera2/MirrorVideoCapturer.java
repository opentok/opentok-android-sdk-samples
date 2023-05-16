package com.tokbox.sample.basicvideocapturercamera2;

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
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import androidx.annotation.RequiresApi;
import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@TargetApi(21)
@RequiresApi(21)
public
class MirrorVideoCapturer extends BaseVideoCapturer implements BaseVideoCapturer.CaptureSwitch {
    private static final int PREFERRED_FACING_CAMERA = CameraMetadata.LENS_FACING_FRONT;
    private static final int PIXEL_FORMAT = ImageFormat.YUV_420_888;
    private static final String TAG = MirrorVideoCapturer.class.getSimpleName();

    private enum CameraState {
        CLOSED,
        CLOSING,
        SETUP,
        OPEN,
        CAPTURE,
        CREATESESSION,
        ERROR
    }

    private final CameraManager cameraManager;
    private CameraDevice camera;
    private HandlerThread cameraThread;
    private Handler cameraThreadHandler;
    private ImageReader cameraFrame;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private CameraInfoCache cameraInfoCache;
    private CameraState cameraState;
    private final Display display;
    private DisplayOrientationCache displayOrientationCache;
    private int cameraIndex;
    private final Size frameDimensions;
    private final int desiredFps;
    private Range<Integer> camFps;
    private final List<RuntimeException> runtimeExceptionList;

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
            append(Publisher.CameraCaptureResolution.HIGH_1080P.ordinal(), new Size(1920, 1080));
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
    private final CameraDevice.StateCallback cameraObserver = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG,"CameraDevice.StateCallback onOpened() enter");
            cameraState = CameraState.OPEN;
            MirrorVideoCapturer.this.camera = camera;
            if (executeAfterCameraOpened != null) {
                executeAfterCameraOpened.run();
            }
            executeAfterCameraOpened = null;
            Log.d(TAG,"CameraDevice.StateCallback onOpened() exit");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG,"CameraDevice.StateCallback onDisconnected() enter");
            try {
                executeAfterClosed = null;
                MirrorVideoCapturer.this.camera.close();
            } catch (NullPointerException e) {
                // does nothing
            }
            Log.d(TAG,"CameraDevice.StateCallback onDisconnected() exit");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG,"CameraDevice.StateCallback onError() enter");
            try {
                MirrorVideoCapturer.this.camera.close();
                // wait for condition variable
            } catch (NullPointerException e) {
                // does nothing
            }
            postAsyncException(new Camera2Exception("Camera Open Error: " + error));
            Log.d(TAG,"CameraDevice.StateCallback onError() exit");
        }

        @Override
        public void onClosed(CameraDevice camera) {
            Log.d(TAG,"CameraDevice.StateCallback onClosed() enter.");
            super.onClosed(camera);
            cameraState = CameraState.CLOSED;
            MirrorVideoCapturer.this.camera = null;

            if (executeAfterClosed != null) {
                executeAfterClosed.run();
            }
            executeAfterClosed = null;
            Log.d(TAG,"CameraDevice.StateCallback onClosed() exit.");
        }
    };

    private final ImageReader.OnImageAvailableListener frameObserver = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try {
                Image frame = reader.acquireNextImage();
                if (frame == null
                        || (frame.getPlanes().length > 0 && frame.getPlanes()[0].getBuffer() == null)
                        || (frame.getPlanes().length > 1 && frame.getPlanes()[1].getBuffer() == null)
                        || (frame.getPlanes().length > 2 && frame.getPlanes()[2].getBuffer() == null))
                {
                    Log.d(TAG,"onImageAvailable frame provided has no image data");
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
            } catch (IllegalStateException e) {
                Log.e(TAG,"ImageReader.acquireNextImage() throws error !");
                throw (new Camera2Exception(e.getMessage()));
            }
        }
    };

    private final CameraCaptureSession.StateCallback captureSessionObserver =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG,"CameraCaptureSession.StateCallback onConfigured() enter.");
                    try {
                        cameraState = CameraState.CAPTURE;
                        captureSession = session;
                        CaptureRequest captureRequest = captureRequestBuilder.build();
                        captureSession.setRepeatingRequest(captureRequest, captureNotification, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG,"CameraCaptureSession.StateCallback onConfigured() exit.");
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d(TAG,"CameraCaptureSession.StateCallback onFailed() enter.");
                    cameraState = CameraState.ERROR;
                    postAsyncException(new Camera2Exception("Camera session configuration failed"));
                    Log.d(TAG,"CameraCaptureSession.StateCallback onFailed() exit.");
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    Log.d(TAG,"CameraCaptureSession.StateCallback onClosed() enter.");
                    if (camera != null) {
                        camera.close();
                    }
                    Log.d(TAG,"CameraCaptureSession.StateCallback onClosed() exit.");
                }
            };

    private final CameraCaptureSession.CaptureCallback captureNotification =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
            };


    /* caching of camera characteristics & display orientation for performance */
    private static class CameraInfoCache {
        private final CameraCharacteristics   info;
        private boolean                 frontFacing = false;
        private int                     sensorOrientation = 0;

        public CameraInfoCache(CameraCharacteristics info) {
            this.info    = info;
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
        private final Display display;
        private final Handler handler;

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
    public MirrorVideoCapturer(Context ctx,
                                Publisher.CameraCaptureResolution resolution,
                                Publisher.CameraCaptureFrameRate fps) {
        cameraManager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        camera = null;
        cameraState = CameraState.CLOSED;
        frameDimensions = resolutionTable.get(resolution.ordinal());
        desiredFps = frameRateTable.get(fps.ordinal());
        runtimeExceptionList = new ArrayList<RuntimeException>();
        try {
            String camId = selectCamera(PREFERRED_FACING_CAMERA);
            /* if default camera facing direction is not found, use first camera */
            if (null == camId && (0 < cameraManager.getCameraIdList().length)) {
                camId = cameraManager.getCameraIdList()[0];
            }
            cameraIndex = findCameraIndex(camId);
            initCameraFrame();
        } catch (CameraAccessException e) {
            throw new Camera2Exception(e.getMessage());
        }
    }

    private void doInit() {
        Log.d(TAG,"doInit() enter");
        cameraInfoCache = null;
        // start camera looper thread
        startCamThread();
        // start display orientation polling
        startDisplayOrientationCache();
        // open selected camera
        initCamera();
        Log.d(TAG,"doInit() exit");
    }

    /**
     * Initializes the video capturer.
     */
    @Override
    public synchronized void init() {
        Log.d(TAG,"init() enter");

        if (cameraState == CameraState.CLOSING) {
            executeAfterClosed = () -> doInit();
        } else {
            doInit();
        }
        cameraState = CameraState.SETUP;
        Log.d(TAG,"init() exit");

    }

    private int doStartCapture() {
        Log.d(TAG,"doStartCapture() enter");
        cameraState = CameraState.CREATESESSION;
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
                        null
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
                        null
                );
            }
        } catch (CameraAccessException e) {
            throw new Camera2Exception(e.getMessage());
        }
        Log.d(TAG,"doStartCapture() exit");
        return 0;
    }

    /**
     * Starts capturing video.
     */
    @Override
    public synchronized int startCapture() {
        Log.d(TAG,"startCapture() enter (cameraState: " + cameraState + ")");
        Runnable resume = () -> {
            initCamera();
            scheduleStartCapture();
        };
        if (cameraState == CameraState.CLOSING) {
            executeAfterClosed = resume;
        } else if (cameraState == CameraState.CLOSED) {
            resume.run();
        } else {
            scheduleStartCapture();
        }
        Log.d(TAG,"startCapture() exit");
        return 0;
    }

    /**
     * Starts capturing video.
     */
    public synchronized int scheduleStartCapture() {
        Log.d(TAG,"scheduleStartCapture() enter (cameraState: " + cameraState + ")");
        if (null != camera && CameraState.OPEN == cameraState) {
            return doStartCapture();
        } else if (CameraState.SETUP == cameraState) {
            Log.d(TAG,"camera not yet ready, queuing the start until camera is opened.");
            executeAfterCameraOpened = this::doStartCapture;
        } else if (CameraState.CREATESESSION == cameraState) {
            Log.d(TAG,"Camera session creation already requested");
        }
        else {
            Log.d(TAG,"Start Capture called before init successfully completed.");
        }
        Log.d(TAG,"scheduleStartCapture() exit");
        return 0;
    }

    /**
     * Stops capturing video.
     */
    @Override
    public synchronized int stopCapture() {
        Log.d(TAG,"stopCapture enter");
        if (null != camera && null != captureSession && CameraState.CLOSED != cameraState) {
            cameraState = CameraState.CLOSING;
            try {
                captureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            captureSession.close();
            cameraInfoCache = null;
        } else if (null != camera && CameraState.OPEN == cameraState) {
            cameraState = CameraState.CLOSING;
            camera.close();
        } else if (CameraState.SETUP == cameraState) {
            /*
            executeAfterCameraOpened = () -> {
                cameraState = CameraState.CLOSING;
                if (camera != null) {
                    camera.close();
                }
            };

             */
            executeAfterCameraOpened = null;
        }
        Log.d(TAG,"stopCapture exit");
        return 0;
    }

    /**
     * Destroys the BaseVideoCapturer object.
     */
    @Override
    public synchronized void destroy() {
        Log.d(TAG,"destroy() enter");

        /* stop display orientation polling */
        stopDisplayOrientationCache();

        /* stop camera message thread */
        stopCamThread();

        /* close ImageReader here */
        cameraFrame.close();
        Log.d(TAG,"destroy() exit");
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
        CaptureSettings captureSettings = new CaptureSettings();
        captureSettings.fps = desiredFps;
        captureSettings.width = (null != cameraFrame) ? cameraFrame.getWidth() : 0;
        captureSettings.height = (null != cameraFrame) ? cameraFrame.getHeight() : 0;
        captureSettings.format = BaseVideoCapturer.NV21;
        captureSettings.expectedDelay = 0;
        return captureSettings;
    }

    /**
     * Call this method when the activity pauses. When you override this method, implement code
     * to respond to the activity being paused. For example, you may pause capturing audio or video.
     *
     * @see #onResume()
     */
    @Override
    public synchronized void onPause() {
        // PublisherKit.onPause() already calls setPublishVideo(false), which stops the camera
        // Nothing to do here
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
        // PublisherKit.onResume() already calls setPublishVideo(true), which resumes the camera
        // Nothing to do here
    }

    private boolean isDepthOutputCamera(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (capabilities != null) {
            for (int capability : capabilities) {
                if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                    Log.d(TAG," REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT => TRUE");
                    return true;
                }
            }
        }
        Log.d(TAG," REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT => FALSE");
        return false;
    }

    private boolean isBackwardCompatible(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (capabilities != null) {
            for (int capability : capabilities) {
                if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                    Log.d(TAG," REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE => TRUE");
                    return true;
                }
            }
        }
        Log.d(TAG," REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE => FALSE");
        return false;
    }

    private Size[] getCameraOutputSizes(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap dimMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        return dimMap.getOutputSizes(PIXEL_FORMAT);
    }

    private int getNextSupportedCameraIndex() throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        int numCameraIds = cameraIds.length;

        // Cycle through all the cameras to find the next one with supported
        // outputs
        for (int i = 0; i < numCameraIds; ++i) {
            // We use +1 so that the algorithm will rollover and check the
            // current camera too.  At minimum, the current camera *should* have
            // supported outputs.
            int nextCameraIndex = (cameraIndex + i + 1) % numCameraIds;
            Size[] outputSizes = getCameraOutputSizes(cameraIds[nextCameraIndex]);
            boolean hasSupportedOutputs = outputSizes != null && outputSizes.length > 0;

            // OPENTOK-48451. Best guess is that the crash is happening when sdk is
            // trying to open depth sensor cameras while doing cycleCamera() function.
            boolean isDepthOutputCamera = isDepthOutputCamera(cameraIds[nextCameraIndex]);
            boolean isBackwardCompatible = isBackwardCompatible(cameraIds[nextCameraIndex]);

            if (hasSupportedOutputs && isBackwardCompatible && !isDepthOutputCamera) {
                return nextCameraIndex;
            }
        }

        // No supported cameras found
        return -1;
    }

    @Override
    public synchronized void cycleCamera() {
        try {
            int nextCameraIndex = getNextSupportedCameraIndex();
            boolean canSwapCamera = nextCameraIndex != -1;

            // I think all devices *should* have at least one camera with
            // supported outputs, but adding this just in case.
            if (!canSwapCamera) {
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "No cameras with supported outputs found");
            }

            cameraIndex = nextCameraIndex;
            swapCamera(cameraIndex);
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
        Log.d(TAG,"swapCamera() enter");

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
                    initCameraFrame();
                    initCamera();
                    startCapture();
                    break;
                case SETUP:
                default:
                    break;
            }
        };
        Log.d(TAG,"swapCamera() exit");
    }

    private boolean isFrontCamera() {
        return (cameraInfoCache != null) && cameraInfoCache.isFrontFacing();
    }

    private void startCamThread() {
        Log.d(TAG,"startCamThread() enter");
        cameraThread = new HandlerThread("Camera2VideoCapturer-Camera-Thread");
        cameraThread.start();
        cameraThreadHandler = new Handler(cameraThread.getLooper());
        Log.d(TAG,"startCamThread() exit");
    }

    private void stopCamThread() {
        Log.d(TAG,"stopCamThread() enter");
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
        Log.d(TAG,"stopCamThread() exit");
    }

    private String selectCamera(int lenseDirection) throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics info = cameraManager.getCameraCharacteristics(id);
            /* discard cameras that don't face the right direction */
            if (lenseDirection == info.get(CameraCharacteristics.LENS_FACING)) {
                Log.d(TAG,"selectCamera() Direction the camera faces relative to device screen: " + info.get(CameraCharacteristics.LENS_FACING));
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

    private Size selectPreferredSize(String camId, final int width, final int height)
            throws CameraAccessException {
        Size[] outputSizeArray = getCameraOutputSizes(camId);
        List<Size> sizeLst = new ArrayList<Size>();
        Collections.addAll(sizeLst, outputSizeArray);
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
        if (cameraInfoCache != null) {
            int cameraRotation = displayOrientationCache.getOrientation();
            int cameraOrientation = cameraInfoCache.sensorOrientation();
            if (!cameraInfoCache.isFrontFacing()) {
                return Math.abs((cameraRotation - cameraOrientation) % 360);
            } else {
                return (cameraRotation + cameraOrientation + 360) % 360;
            }
        } else {
            return 0;
        }
    }

    private void initCameraFrame() {
        Log.d(TAG,"initCameraFrame() enter.");
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            String camId = cameraIdList[cameraIndex];
            Size preferredSize = selectPreferredSize(
                    camId,
                    frameDimensions.getWidth(),
                    frameDimensions.getHeight()
            );
            if (cameraFrame != null)
                cameraFrame.close();
            cameraFrame = ImageReader.newInstance(
                    preferredSize.getWidth(),
                    preferredSize.getHeight(),
                    PIXEL_FORMAT,
                    3
            );
        } catch (CameraAccessException exp) {
            throw new Camera2Exception(exp.getMessage());
        }
        Log.d(TAG,"initCameraFrame() exit.");
    }

    @SuppressLint("all")
    private void initCamera() {
        Log.d(TAG,"initCamera() enter.");
        try {
            cameraState = CameraState.SETUP;
            // find desired camera & camera ouput size
            String[] cameraIdList = cameraManager.getCameraIdList();
            String camId = cameraIdList[cameraIndex];
            camFps = selectCameraFpsRange(camId, desiredFps);
            cameraFrame.setOnImageAvailableListener(frameObserver, cameraThreadHandler);
            cameraInfoCache = new CameraInfoCache(cameraManager.getCameraCharacteristics(camId));
            cameraManager.openCamera(camId, cameraObserver, null);
        } catch (CameraAccessException exp) {
            throw new Camera2Exception(exp.getMessage());
        }
        Log.d(TAG,"initCamera() exit.");
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