package com.tokbox.sample.basicvideochat_connectionservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.opentok.android.BaseAudioDevice;
import com.opentok.android.OtLog;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class AdvancedAudioDevice extends BaseAudioDevice {

    private final OtLog.LogToken log = new OtLog.LogToken(this);

    private static final int NUM_CHANNELS_CAPTURING = 1;
    private static final int NUM_CHANNELS_RENDERING = 1;
    private static final int STEREO_CHANNELS = 2;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE_IN_BYTES = 2;
    private static final int DEFAULT_SAMPLES_PER_BUFFER = (DEFAULT_SAMPLE_RATE / 1000) * 10; // 10ms
    private static final int DEFAULT_BUFFER_SIZE = SAMPLE_SIZE_IN_BYTES * DEFAULT_SAMPLES_PER_BUFFER * STEREO_CHANNELS;
    // Max 10 ms @ 48 kHz - Stereo
    private static final int DEFAULT_START_RENDERER_AND_CAPTURER_DELAY = 5 * 1000;
    private final Context context;

    private AudioTrack audioTrack;
    private AudioRecord audioRecord;

    // Capture & render buffers
    private ByteBuffer playBuffer;
    private ByteBuffer recBuffer;
    private byte[] tempBufPlay;
    private byte[] tempBufRec;

    private final ReentrantLock rendererLock = new ReentrantLock(true);
    private final Condition renderEvent = rendererLock.newCondition();
    private volatile boolean isRendering = false;
    private volatile boolean shutdownRenderThread = false;

    private final ReentrantLock captureLock = new ReentrantLock(true);
    private final Condition captureEvent = captureLock.newCondition();
    private volatile boolean isCapturing = false;
    private volatile boolean shutdownCaptureThread = false;

    private final AudioSettings captureSettings;
    private final AudioSettings rendererSettings;
    private NoiseSuppressor noiseSuppressor;
    private AcousticEchoCanceler echoCanceler;

    // Capturing delay estimation
    private int estimatedCaptureDelay = 0;

    // Rendering delay estimation
    private int bufferedPlaySamples = 0;
    private int playPosition = 0;
    private int estimatedRenderDelay = 0;

    private final AudioManager audioManager;
    private final AudioManagerMode audioManagerMode = new AudioManagerMode();

    private int outputSamplingRate = DEFAULT_SAMPLE_RATE;
    private final int captureSamplingRate = DEFAULT_SAMPLE_RATE;
    private int samplesPerBuffer = DEFAULT_SAMPLES_PER_BUFFER;

    // For headset receiver.
    private static final String HEADSET_PLUG_STATE_KEY = "state";

    private TelephonyManager telephonyManager;

    // Handles all tasks related to Bluetooth headset devices.
    public BluetoothManager bluetoothManager;

    private boolean isResumingAfterPaused;

    public enum OutputType {
        SPEAKER_PHONE,
        EAR_PIECE,
        HEAD_PHONES,
        BLUETOOTH
    }

    private OutputType audioOutputType = OutputType.SPEAKER_PHONE;

    public OutputType getOutputType() {
        return audioOutputType;
    }

    public void setOutputType(OutputType type) {
        log.d("setOutputType(). Setting output type to " + type);
        audioOutputType = type;
    }

    private boolean sdkHandlesAudioFocus = true;
    private boolean audioFocusActive = false;

    private static class AudioManagerMode {
        private int oldMode;
        private int naquire;
        private final OtLog.LogToken log = new OtLog.LogToken(this);

        AudioManagerMode() {
            oldMode = 0;
            naquire = 0;
        }

        void acquireMode(AudioManager audioManager) {
            log.d("AudioManagerMode.acquireMode() called");
            if (0 == naquire++) {
                oldMode = audioManager.getMode();
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        }

        void releaseMode(AudioManager audioManager) {
            log.d("AudioManagerMode.releaseMode() called");
            if (0 == --naquire) {
                audioManager.setMode(oldMode);
            }
        }
    }

    private static class AudioState {
        private int lastStreamVolume = 0;
        private int lastKnownFocusState = 0;
        private OutputType lastOutputType = OutputType.SPEAKER_PHONE;

        private final OtLog.LogToken log = new OtLog.LogToken(this);

        int getLastStreamVolume() {
            return lastStreamVolume;
        }

        void setLastStreamVolume(int lastStreamVolume) {
            this.lastStreamVolume = lastStreamVolume;
        }

        int getLastKnownFocusState() {
            return lastKnownFocusState;
        }

        void setLastKnownFocusState(int lastKnownFocusState) {
            this.lastKnownFocusState = lastKnownFocusState;
        }

        OutputType getPreviousOutputType() {
            log.d("AudioState.getPreviousOutputType() = " + this.lastOutputType);
            return this.lastOutputType;
        }

        void setPreviousOutputType(OutputType lastOutputType) {
            log.d("AudioState.setPreviousOutputType() = " + lastOutputType);
            this.lastOutputType = lastOutputType;
        }
    }

    private final AudioState audioState = new AudioState();

    private final BroadcastReceiver headsetBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra(HEADSET_PLUG_STATE_KEY, 0) == 1) {
                    log.d("headsetBroadcastReceiver.onReceive():  Headphones connected");
                    audioState.setPreviousOutputType(getOutputType());
                    setOutputType(OutputType.HEAD_PHONES);
                    audioManager.setSpeakerphoneOn(false);
                    bluetoothManager.stopBluetoothSCO();
                } else {
                    log.d("headsetBroadcastReceiver.onReceive():  Headphones disconnected");
                    //Check if it's a disconnect after connected state. This will be called after headset registration.
                    if (getOutputType() == OutputType.HEAD_PHONES) {
                        OutputType type = audioState.getPreviousOutputType();
                        switch (type) {
                            case BLUETOOTH:
                                bluetoothManager.startBluetoothSCO();
                                //No need to call setOutputType here as it will be done after
                                //bluetoothBroadcastReceiver.onReceive(): AudioManager.SCO_AUDIO_STATE_CONNECTED is invoked.
                                break;

                            case SPEAKER_PHONE:
                                audioManager.setSpeakerphoneOn(true);
                                setOutputType(type);
                                break;

                            case EAR_PIECE:
                                audioManager.setSpeakerphoneOn(false);
                                setOutputType(type);
                                break;

                            default:
                                log.e("This should not happen :(");
                                break;
                        }
                    }
                }
            }
        }
    };

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            log.d("PhoneStateListener.onCallStateChanged() enter <-");
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {

                case TelephonyManager.CALL_STATE_IDLE:
                    //Initial state
                    log.d("PhoneStateListener.onCallStateChanged(): TelephonyManager.CALL_STATE_IDLE");
                    // We delay a bit here the action of start capturing and rendering again because Android has to
                    // finish routing audio to the earpiece. It is an Android behaviour we have to deal with.
                    new Handler().postDelayed(() -> startRendererAndCapturer(), DEFAULT_START_RENDERER_AND_CAPTURER_DELAY);
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    // Incoming call Ringing
                    log.d("PhoneStateListener.onCallStateChanged(): TelephonyManager.CALL_STATE_RINGING");
                    stopRendererAndCapturer();
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Outgoing Call | Accepted incoming call
                    log.d("PhoneStateListener.onCallStateChanged(): TelephonyManager.CALL_STATE_OFFHOOK");
                    stopRendererAndCapturer();
                    break;

                default:
                    log.d("PhoneStateListener.onCallStateChanged() default");
                    break;
            }
            log.d("PhoneStateListener.onCallStateChanged() exit ->");
        }
    };

    private boolean wasRendering;
    private boolean wasCapturing;

    private void startRendererAndCapturer() {
        log.d("startRendererAndCapturer() enter <-");

        if (wasRendering) {
            startRenderer();
        }

        if (wasCapturing) {
            startCapturer();
        }

        log.d("startRendererAndCapturer() exit ->");
    }

    private void stopRendererAndCapturer() {
        log.d("stopRendererAndCapturer() enter <-");

        if (isRendering) {
            stopRenderer();
            wasRendering = true;
        }

        if (isCapturing) {
            stopCapturer();
            wasCapturing = true;
        }

        log.d("stopRendererAndCapturer() exit ->");
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange() enter <-");
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange(" + focusChange + "): ");
                    //Check if coming back from a complete loss or a transient loss
                    switch (audioState.getLastKnownFocusState()) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                                    audioState.getLastStreamVolume(), 0);
                            break;
                        default:
                            log.d("focusChange = " + focusChange);
                            break;
                    }
                    setOutputType(audioState.getPreviousOutputType());
                    bluetoothManager.forceInvokeConnectBluetooth();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    // -1 Loss for indefinite time
                    log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange(" + focusChange + "): AudioManager.AUDIOFOCUS_LOSS");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // -2 Loss for short duration
                    log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange(" + focusChange + "): AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // -3 stay quite in background
                    log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange(" + focusChange + "): AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    audioState.setLastStreamVolume(audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0);
                    break;
                case AudioManager.AUDIOFOCUS_NONE:
                    log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange(" + focusChange + "): AudioManager.AUDIOFOCUS_NONE");
                    break;
                default:
                    log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange(" + focusChange + "): default");
                    break;
            }
            audioState.setPreviousOutputType(getOutputType());
            audioState.setLastKnownFocusState(focusChange);
            log.d("AudioManager.OnAudioFocusChangeListener.onAudioFocusChange() exit ->");
        }
    };

    public AdvancedAudioDevice(Context context, boolean requestAudioFocus) {
        log.d("DefaultAudioDevice() enter " + this);
        this.context = context;
        this.sdkHandlesAudioFocus = requestAudioFocus;
        try {
            recBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        } catch (Exception e) {
            log.e(e.getMessage());
        }
        tempBufRec = new byte[DEFAULT_BUFFER_SIZE];

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        bluetoothManager = BluetoothManager.create(context, this);

        int outputBufferSize = DEFAULT_BUFFER_SIZE;

        try {
            outputSamplingRate = Integer.parseInt(
                    audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
        } catch (NumberFormatException numberFormatException) {
            log.e("DefaultAudioDevice(): " + numberFormatException.getMessage());
        } finally {
            if (outputSamplingRate == 0) {
                outputSamplingRate = DEFAULT_SAMPLE_RATE;
            }
        }
        try {
            samplesPerBuffer = Integer.parseInt(
                    audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
            outputBufferSize = SAMPLE_SIZE_IN_BYTES
                    * samplesPerBuffer
                    * NUM_CHANNELS_RENDERING;
        } catch (NumberFormatException numberFormatException) {
            log.e("DefaultAudioDevice(): " + numberFormatException.getMessage());
        } finally {
            if (outputBufferSize == 0) {
                outputBufferSize = DEFAULT_BUFFER_SIZE;
                samplesPerBuffer = DEFAULT_SAMPLES_PER_BUFFER;
            }
        }

        try {
            playBuffer = ByteBuffer.allocateDirect(outputBufferSize);
        } catch (Exception e) {
            log.e(e.getMessage());
        }

        tempBufPlay = new byte[outputBufferSize];

        captureSettings = new AudioSettings(captureSamplingRate,
                NUM_CHANNELS_CAPTURING);
        rendererSettings = new AudioSettings(outputSamplingRate,
                NUM_CHANNELS_RENDERING);
        try {
            telephonyManager = (TelephonyManager) context.getSystemService(Context
                    .TELEPHONY_SERVICE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        isPhoneStateListenerRegistered = false;
        wasCapturing = false;
        wasRendering = false;
        isResumingAfterPaused = false;
        log.d("DefaultAudioDevice() exit  " + this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean initCapturer() {
        log.d("initCapturer() enter <-");

        // get the minimum buffer size that can be used
        int minRecBufSize = AudioRecord.getMinBufferSize(
                captureSettings.getSampleRate(),
                NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        // double size to be more safe
        int recBufSize = minRecBufSize * 2;

        // release the object
        if (noiseSuppressor != null) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
        if (echoCanceler != null) {
            echoCanceler.release();
            echoCanceler = null;
        }
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }

        try {
            audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
                    captureSettings.getSampleRate(),
                    NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO
                            : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, recBufSize);
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            }
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // Check that the audioRecord is ready to be used.
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            String errorDescription = String.format(Locale.getDefault(), "Audio capture could not be initialized.\n" +
                            "Requested parameters\n" +
                            "  Sampling Rate: %d\n" +
                            "  Number of channels: %d\n" +
                            "  Buffer size: %d\n",
                    captureSettings.getSampleRate(),
                    captureSettings.getNumChannels(),
                    minRecBufSize);
            log.e(errorDescription);
            throw new RuntimeException(errorDescription);
        }

        registerPhoneStateListener();

        shutdownCaptureThread = false;
        new Thread(captureThread).start();
        log.d("initCapturer() exit ->");
        return true;
    }

    @Override
    public boolean destroyCapturer() {
        log.d("destroyCapturer() enter <-");

        captureLock.lock();
        // release the object
        if (null != echoCanceler) {
            echoCanceler.release();
            echoCanceler = null;
        }
        if (null != noiseSuppressor) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
        audioRecord.release();
        audioRecord = null;
        shutdownCaptureThread = true;
        captureEvent.signal();

        captureLock.unlock();

        unRegisterPhoneStateListener();
        wasCapturing = false;
        log.d("destroyCapturer() exit ->");
        return true;
    }

    public int getEstimatedCaptureDelay() {
        return estimatedCaptureDelay;
    }

    @Override
    public boolean startCapturer() {

        log.d("startCapturer() enter <-");

        if (audioRecord == null) {
            throw new IllegalStateException("startCapturer(): startRecording() called on an "
                    + "uninitialized AudioRecord.");
        }
        try {
            audioRecord.startRecording();

        } catch (IllegalStateException e) {
            throw new RuntimeException(e.getMessage());
        }

        captureLock.lock();
        isCapturing = true;
        captureEvent.signal();
        captureLock.unlock();
        if(sdkHandlesAudioFocus) {
            audioManagerMode.acquireMode(audioManager);
        }
        log.d("startCapturer() exit ->");
        return true;
    }

    @Override
    public boolean stopCapturer() {
        log.d("stopCapturer() enter <-");

        if (audioRecord == null) {
            throw new IllegalStateException("stopCapturer(): stop() called on an uninitialized AudioRecord.");
        }
        captureLock.lock();
        try {
            // Only stop if we are recording.
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            // Ensure we always unlock
            isCapturing = false;
            captureLock.unlock();
        }
        if(sdkHandlesAudioFocus) {
            audioManagerMode.releaseMode(audioManager);
        }
        log.d("stopCapturer() exit ->");
        return true;
    }

    private final Runnable captureThread = () -> {
        int samplesToRec = captureSamplingRate / 100;
        int samplesRead;

        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        } catch (Exception e) {
            log.e("android.os.Process.setThreadPriority(): " + e.getMessage());
        }

        while (!shutdownCaptureThread) {
            captureLock.lock();
            try {
                if (!AdvancedAudioDevice.this.isCapturing) {
                    captureEvent.await();
                    continue;
                } else {
                    if (audioRecord == null) {
                        continue;
                    }
                    int lengthInBytes = (samplesToRec << 1) * NUM_CHANNELS_CAPTURING;
                    int readBytes = audioRecord.read(tempBufRec, 0, lengthInBytes);
                    if (readBytes >= 0) {
                        recBuffer.rewind();
                        recBuffer.put(tempBufRec);
                        samplesRead = (readBytes >> 1) / NUM_CHANNELS_CAPTURING;
                    } else {
                        switch (readBytes) {
                            case AudioRecord.ERROR_BAD_VALUE:
                                throw new RuntimeException("captureThread(): AudioRecord.ERROR_BAD_VALUE");
                            case AudioRecord.ERROR_INVALID_OPERATION:
                                throw new RuntimeException("captureThread(): AudioRecord.ERROR_INVALID_OPERATION");
                            case AudioRecord.ERROR:
                            default:
                                throw new RuntimeException("captureThread(): AudioRecord.ERROR or default");
                        }
                    }
                }
            } catch (Exception e) {
                log.e("Audio device capture error: " + e.getMessage());
                return;
            } finally {
                // Ensure we always unlock
                captureLock.unlock();
            }
            getAudioBus().writeCaptureData(recBuffer, samplesRead);
            estimatedCaptureDelay = samplesRead * 1000 / captureSamplingRate;
        }
    };

    public boolean requestAudioFocus() {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                // Use the music stream.
                AudioManager.STREAM_VOICE_CALL,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            log.d("initRenderer(): AudioManager.AUDIOFOCUS_REQUEST_GRANTED");
            audioFocusActive = true;
        } else {
            log.e("initRenderer(): AudioManager.AUDIOFOCUS_REQUEST_FAILED");
        }

        return audioFocusActive;
    }

    @Override
    public boolean initRenderer() {
        log.d("initRenderer() enter <-");

        if(sdkHandlesAudioFocus) {
            requestAudioFocus();
        }

        // get the minimum buffer size that can be used
        int minPlayBufSize = AudioTrack.getMinBufferSize(
                rendererSettings.getSampleRate(),
                NUM_CHANNELS_RENDERING == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        // release the object
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }

        try {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    rendererSettings.getSampleRate(),
                    (NUM_CHANNELS_RENDERING == 1)
                            ? AudioFormat.CHANNEL_OUT_MONO
                            : AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minPlayBufSize >= 6000 ? minPlayBufSize : minPlayBufSize * 2,
                    AudioTrack.MODE_STREAM
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        // Check that the audioRecord is ready to be used.
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new RuntimeException("Audio renderer not initialized " + rendererSettings.getSampleRate());
        }

        bufferedPlaySamples = 0;

        registerPhoneStateListener();

        shutdownRenderThread = false;
        new Thread(renderThread).start();
        log.d("initRenderer() exit ->");
        return true;
    }

    private void destroyAudioTrack() {
        rendererLock.lock();
        audioTrack.release();
        audioTrack = null;
        shutdownRenderThread = true;
        renderEvent.signal();
        rendererLock.unlock();
    }

    @Override
    public boolean destroyRenderer() {
        log.d("destroyRenderer() enter <-");

        destroyAudioTrack();
        unregisterHeadsetReceiver();
        bluetoothManager.unregisterBtReceiver();
        audioManager.setSpeakerphoneOn(false);
        if(sdkHandlesAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            audioFocusActive = false;
        }

        unRegisterPhoneStateListener();
        wasRendering = false;
        log.d("destroyRenderer() exit ->");

        return true;
    }

    public int getEstimatedRenderDelay() {
        return estimatedRenderDelay;
    }

    @Override
    public boolean startRenderer() {
        log.d("startRenderer() enter <-");

        if(audioFocusActive) {
            bluetoothManager.forceInvokeConnectBluetooth();

            // Enable speakerphone unless headset is connected.
            if (bluetoothManager.isBluetoothConnected() == false) {
                if (isWiredHeadSetConnected()) {
                    log.d("startRenderer(): Turn off Speaker phone");
                    audioManager.setSpeakerphoneOn(false);
                } else {
                    if (getOutputType() == OutputType.SPEAKER_PHONE && !bluetoothManager.isBluetoothConnected()) {
                        log.d("startRenderer(): Turn on Speaker phone");
                        audioManager.setSpeakerphoneOn(true);
                    }
                }
            }

            // Start playout.
            if (audioTrack == null) {
                throw new IllegalStateException("startRenderer(): play() called on uninitialized AudioTrack.");
            }
            try {
                audioTrack.play();
            } catch (IllegalStateException e) {
                throw new RuntimeException(e.getMessage());
            }
            rendererLock.lock();
            isRendering = true;
            renderEvent.signal();
            rendererLock.unlock();
            if (sdkHandlesAudioFocus) {
                audioManagerMode.acquireMode(audioManager);
            }
            registerHeadsetReceiver();
            log.d("startRenderer() exit ->");
        }
        return true;
    }


    @Override
    public boolean stopRenderer() {
        log.d("stopRenderer() enter <-");

        if(audioFocusActive) {
            if (audioTrack == null) {
                throw new IllegalStateException("stopRenderer(): stop() called on uninitialized AudioTrack.");
            }
            rendererLock.lock();
            try {
                // Only stop if we are playing.
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop();

                }
                audioTrack.flush();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            } finally {
                isRendering = false;
                rendererLock.unlock();
            }
            if (sdkHandlesAudioFocus) {
                audioManagerMode.releaseMode(audioManager);
            }

            unregisterHeadsetReceiver();
            log.d("stopRenderer() exit ->");
        }
        return true;
    }

    private final Runnable renderThread = () -> {
        int samplesToPlay = samplesPerBuffer;
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        } catch (Exception e) {
            log.e("android.os.Process.setThreadPriority(): " + e.getMessage());
        }

        while (!shutdownRenderThread) {
            rendererLock.lock();
            try {
                if (!AdvancedAudioDevice.this.isRendering) {
                    renderEvent.await();
                } else {
                    rendererLock.unlock();

                    // Don't lock on audioBus calls
                    playBuffer.clear();
                    int samplesRead = getAudioBus().readRenderData(playBuffer, samplesToPlay);

                    rendererLock.lock();

                    // After acquiring the lock again we must check if we are still playing
                    if (audioTrack == null || !AdvancedAudioDevice.this.isRendering) {
                        continue;
                    }

                    int bytesRead = (samplesRead << 1) * NUM_CHANNELS_RENDERING;
                    playBuffer.get(tempBufPlay, 0, bytesRead);

                    int bytesWritten = audioTrack.write(tempBufPlay, 0, bytesRead);
                    if (bytesWritten > 0) {
                        // increase by number of written samples
                        bufferedPlaySamples += (bytesWritten >> 1) / NUM_CHANNELS_RENDERING;

                        // decrease by number of played samples
                        int pos = audioTrack.getPlaybackHeadPosition();
                        if (pos < playPosition) {
                            // wrap or reset by driver
                            playPosition = 0;
                        }
                        bufferedPlaySamples -= (pos - playPosition);
                        playPosition = pos;

                        // we calculate the estimated delay based on the buffered samples
                        estimatedRenderDelay = bufferedPlaySamples * 1000 / outputSamplingRate;
                    } else {
                        switch (bytesWritten) {
                            case AudioTrack.ERROR_BAD_VALUE:
                                throw new RuntimeException(
                                        "renderThread(): AudioTrack.ERROR_BAD_VALUE");
                            case AudioTrack.ERROR_INVALID_OPERATION:
                                throw new RuntimeException(
                                        "renderThread(): AudioTrack.ERROR_INVALID_OPERATION");
                            case AudioTrack.ERROR:
                            default:
                                throw new RuntimeException(
                                        "renderThread(): AudioTrack.ERROR or default");
                        }
                    }
                }
            } catch (Exception e) {
                log.e("Audio device capture error: " + e.getMessage());
                return;
            } finally {
                rendererLock.unlock();
            }
        }
    };

    @Override
    public AudioSettings getCaptureSettings() {
        return this.captureSettings;
    }

    @Override
    public AudioSettings getRenderSettings() {
        return this.rendererSettings;
    }

    /**
     * Communication modes handling.
     */
    public boolean setOutputMode(OutputMode mode) {
        //This is public API and also called during initialization
        log.d("setOutputMode(). Audio Output mode set to --> " + mode);
        super.setOutputMode(mode);
        if (OutputMode.SpeakerPhone == mode) {
            audioState.setPreviousOutputType(getOutputType());
            setOutputType(OutputType.SPEAKER_PHONE);
            audioManager.setSpeakerphoneOn(true);
            bluetoothManager.stopBluetoothSCO();
        } else {
            //OutputMode.Handset. This state will be either Bluetooth or earpiece.
            audioManager.setSpeakerphoneOn(false);
            if (audioState.getPreviousOutputType() == OutputType.BLUETOOTH ||
                    getBluetoothState() == BluetoothState.Connected) {
                setOutputType(OutputType.BLUETOOTH);
                bluetoothManager.startBluetoothSCO();
            } else {
                audioState.setPreviousOutputType(getOutputType());
                setOutputType(OutputType.EAR_PIECE);
                bluetoothManager.stopBluetoothSCO();
            }
        }
        return true;
    }

    private boolean isHeadsetReceiverRegistered;

    private synchronized void registerHeadsetReceiver() {
        log.d("registerHeadsetReceiver() enter ... isHeadsetReceiverRegistered = " + isHeadsetReceiverRegistered);

        if (!isHeadsetReceiverRegistered) {
            context.registerReceiver(headsetBroadcastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            isHeadsetReceiverRegistered = true;
        }

        log.d("registerHeadsetReceiver() exit ... isHeadsetReceiverRegistered = " + isHeadsetReceiverRegistered);
    }

    private synchronized void unregisterHeadsetReceiver() {
        log.d("unregisterHeadsetReceiver() enter .. isHeadsetReceiverRegistered = " + isHeadsetReceiverRegistered);

        if (isHeadsetReceiverRegistered) {
            context.unregisterReceiver(headsetBroadcastReceiver);
            isHeadsetReceiverRegistered = false;
        }

        log.d("unregisterHeadsetReceiver() exit .. isHeadsetReceiverRegistered = " + isHeadsetReceiverRegistered);
    }

    private boolean isPhoneStateListenerRegistered;

    //Phone state permissions are required to from Api 31. Adding this check to avoid crash.
    private boolean hasPhoneStatePermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                log.e("Some features may not be available unless the phone permissions has been granted explicitly " +
                        "in the App settings.");
                return false;
            }
        }
        return true;
    }

    private void registerPhoneStateListener() {
        log.d("registerPhoneStateListener() enter");

        if (isPhoneStateListenerRegistered) {
            log.d("phoneStateListener is already registered.");
            return;
        }

        if (!hasPhoneStatePermission()) {
            log.d("No Phone State permissions. Register phoneStateListener cannot " +
                    "be completed.");
            return;
        }

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            isPhoneStateListenerRegistered = true;
        }
        log.d("registerPhoneStateListener() exit");
    }

    private void unRegisterPhoneStateListener() {
        log.d("unRegisterPhoneStateListener() enter.");

        if (!isPhoneStateListenerRegistered) {
            log.d("phoneStateListener is already unregistered.");
            return;
        }

        if (!hasPhoneStatePermission()) {
            log.d("No Phone State permissions. Unregister phoneStateListener cannot " +
                    "be completed.");
            return;
        }

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            isPhoneStateListenerRegistered = false;
        }
        log.d("unRegisterPhoneStateListener() exit.");
    }

    @Override
    public synchronized void onPause() {
        log.d("onPause() enter <-");
        audioState.setPreviousOutputType(getOutputType());
        bluetoothManager.onPause();
        unregisterHeadsetReceiver();
        isResumingAfterPaused = true;
        log.d("onPause() exit ->");
    }

    @Override
    public synchronized void onResume() {
        log.d("onResume() enter. isResumingAfterPaused = " + isResumingAfterPaused);

        /* register handler for phonejack notifications */
        registerHeadsetReceiver();

        /* Handle Bluetooth */
        bluetoothManager.onResume();

        // This condition can happen after user switches to another app and resumes the opentok session.
        if (isResumingAfterPaused) {
            if (getBluetoothState() == BluetoothState.Disconnected) {
                if (isRendering && (audioState.getPreviousOutputType() == OutputType.SPEAKER_PHONE)) {
                    if (!isWiredHeadSetConnected()) {
                        log.d("onResume() - Set Speaker Phone ON True");
                        audioManager.setSpeakerphoneOn(true);
                    }
                }
            }
            isResumingAfterPaused = false;
        }

        log.d("onResume() exit ->");
    }

    @Override
    public BaseAudioDevice.BluetoothState getBluetoothState() {
        return bluetoothManager.getBluetoothState();
    }

    boolean isWiredHeadSetConnected() {
        AudioDeviceInfo[] audioDeviceInfos = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo audioDeviceInfo : audioDeviceInfos) {
            log.d("Detected connected audio output device of type: " + audioDeviceInfo.getType());
            if (audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                log.d("Detected connected wired headset");
                return true;
            }
        }
        return false;
    }

    public void restoreAudioAfterBluetoothDisconnect() {
        log.d("restoreAudioAfterBluetoothDisconnect enter.");
        if (isWiredHeadSetConnected()) {
            setOutputType(AdvancedAudioDevice.OutputType.HEAD_PHONES);
            audioManager.setSpeakerphoneOn(false);
        } else {
            OutputMode outputMode = getOutputMode();
            switch (outputMode) {
                case SpeakerPhone:
                    log.d("Falling back to speaker mode.");
                    setOutputType(AdvancedAudioDevice.OutputType.SPEAKER_PHONE);
                    audioManager.setSpeakerphoneOn(true);
                    break;

                case Handset:
                    log.d("Falling back to handset mode.");
                    setOutputType(AdvancedAudioDevice.OutputType.EAR_PIECE);
                    audioManager.setSpeakerphoneOn(false);
                    break;

                default:
                    log.e("This should not happen. :(");
            }
        }
        log.d("restoreAudioAfterBluetoothDisconnect exit.");
    }
}
