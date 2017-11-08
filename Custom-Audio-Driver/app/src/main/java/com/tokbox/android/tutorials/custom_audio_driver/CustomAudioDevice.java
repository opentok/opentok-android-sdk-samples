package com.tokbox.android.tutorials.custom_audio_driver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import com.opentok.android.BaseAudioDevice;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomAudioDevice extends BaseAudioDevice {

    private final static String LOG_TAG =  CustomAudioDevice.class.getSimpleName();;

    private static final int NUM_CHANNELS_CAPTURING = 1;
    private static final int NUM_CHANNELS_RENDERING = 1;
    private static final int STEREO_CHANNELS = 2;
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE_IN_BYTES = 2;
    private static final int DEFAULT_SAMPLES_PER_BUFFER = (DEFAULT_SAMPLE_RATE / 1000) * 10; // 10ms
    private static final int DEFAULT_BUFFER_SIZE =
            SAMPLE_SIZE_IN_BYTES * DEFAULT_SAMPLES_PER_BUFFER * STEREO_CHANNELS;
    // Max 10 ms @ 48 kHz - Stereo

    private Context context;

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

    private AudioSettings captureSettings;
    private AudioSettings rendererSettings;
    private NoiseSuppressor noiseSuppressor;
    private AcousticEchoCanceler echoCanceler;

    private OutputType audioOutput = OutputType.PHONE_SPEAKERS;

    // Capturing delay estimation
    private int estimatedCaptureDelay = 0;

    // Rendering delay estimation
    private int bufferedPlaySamples = 0;
    private int playPosition = 0;
    private int estimatedRenderDelay = 0;

    private AudioManager audioManager;
    private AudioManagerMode audioManagerMode = new AudioManagerMode();

    private int outputSamplingRate = DEFAULT_SAMPLE_RATE;
    private int captureSamplingRate = DEFAULT_SAMPLE_RATE;
    private int samplesPerBuffer = DEFAULT_SAMPLES_PER_BUFFER;

    // for headset receiver
    private static final String HEADSET_PLUG_STATE_KEY = "state";

    // for bluetooth
    private BluetoothState bluetoothState;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothProfile bluetoothProfile;
    private Object bluetoothLock = new Object();

    private enum BluetoothState {
        DISCONNECTED, CONNECTED
    }

    private enum OutputType {
        PHONE_SPEAKERS,     /* speaker-phone & ear-piece */
        HEADPHONES,
        BLUETOOTH
    }

    private final BroadcastReceiver btStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (null != action && action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothHeadset.STATE_CONNECTED:
                        Log.d(LOG_TAG, "BroadcastReceiver: STATE_CONNECTED");
                        synchronized (bluetoothLock) {
                            if (BluetoothState.DISCONNECTED == bluetoothState) {
                                Log.d(LOG_TAG, "Bluetooth Headset: Connecting SCO");
                                bluetoothState = BluetoothState.CONNECTED;
                                setOutputType(OutputType.BLUETOOTH);
                                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                                audioManager.setBluetoothScoOn(true);
                                startBluetoothSco();
                                audioManager.setSpeakerphoneOn(false);
                            }
                        }
                        break;
                    case BluetoothHeadset.STATE_DISCONNECTED:
                        Log.d(LOG_TAG, "BroadcastReceiver: STATE_DISCONNECTED");
                        synchronized (bluetoothLock) {
                            if (BluetoothState.CONNECTED == bluetoothState) {
                                Log.d(LOG_TAG, "Bluetooth Headset: Disconnecting SCO");
                                bluetoothState = BluetoothState.DISCONNECTED;
                                audioManager.setBluetoothScoOn(false);
                                stopBluetoothSco();
                                if (audioManager.isWiredHeadsetOn()) {
                                    setOutputType(OutputType.HEADPHONES);
                                    audioManager.setSpeakerphoneOn(false);
                                } else {
                                    setOutputType(OutputType.PHONE_SPEAKERS);
                                    audioManager.setSpeakerphoneOn(
                                            getOutputMode() == OutputMode.SpeakerPhone
                                    );
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private final BluetoothProfile.ServiceListener bluetoothProfileListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int type, BluetoothProfile profile) {
                    if  (BluetoothProfile.HEADSET == type) {
                        bluetoothProfile = profile;
                        List<BluetoothDevice> devices = profile.getConnectedDevices();
                        Log.d(LOG_TAG, "Service Proxy Connected");
                        if (!devices.isEmpty()
                                && BluetoothHeadset.STATE_CONNECTED == profile.getConnectionState(devices.get(0))) {
                    /* force a init of bluetooth: the handler will not send a connected event if a
                       device is already connected at the time of proxy connection request. */
                            Intent intent = new Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                            intent.putExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_CONNECTED);
                            btStatusReceiver.onReceive(context, intent);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(int type) {
                    Log.d(LOG_TAG, "Service Proxy Disconnected");
                }
            };

    private static class AudioManagerMode {
        private int oldMode;
        private int naquire;

        public AudioManagerMode() {
            oldMode = 0;
            naquire = 0;
        }

        public void acquireMode(AudioManager audioManager) {
            if (0 == naquire++) {
                oldMode = audioManager.getMode();
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        }

        public void releaseMode(AudioManager audioManager) {
            if (0 == --naquire) {
                audioManager.setMode(oldMode);
            }
        }
    }

    public CustomAudioDevice(Context context) {
        this.context = context;

        try {
            recBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        tempBufRec = new byte[DEFAULT_BUFFER_SIZE];

        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothProfile = null;

        int outputBufferSize = DEFAULT_BUFFER_SIZE;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            try {
                outputSamplingRate = Integer.parseInt(
                        audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
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
            } finally {
                if (outputBufferSize == 0) {
                    outputBufferSize = DEFAULT_BUFFER_SIZE;
                    samplesPerBuffer = DEFAULT_SAMPLES_PER_BUFFER;
                }
            }
        }

        try {
            playBuffer = ByteBuffer.allocateDirect(outputBufferSize);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        tempBufPlay = new byte[outputBufferSize];

        captureSettings = new AudioSettings(captureSamplingRate,
                NUM_CHANNELS_CAPTURING);
        rendererSettings = new AudioSettings(outputSamplingRate,
                NUM_CHANNELS_RENDERING);

    }

    @Override
    public boolean initCapturer() {
        // initalize audio mode
        audioManagerMode.acquireMode(audioManager);

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

        // check that the audioRecord is ready to be used
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new RuntimeException("Audio capture is not initialized " + captureSettings.getSampleRate());
        }

        shutdownCaptureThread = false;
        new Thread(captureThread).start();
        return true;
    }

    @Override
    public boolean destroyCapturer() {
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
        // shutdown audio mode
        audioManagerMode.releaseMode(audioManager);
        return true;
    }

    public int getEstimatedCaptureDelay() {
        return estimatedCaptureDelay;
    }

    @Override
    public boolean startCapturer() {
        // start recording
        try {
            audioRecord.startRecording();

        } catch (IllegalStateException e) {
            throw new RuntimeException(e.getMessage());
        }

        captureLock.lock();
        isCapturing = true;
        captureEvent.signal();
        captureLock.unlock();
        return true;
    }

    @Override
    public boolean stopCapturer() {
        captureLock.lock();
        try {
            // only stop if we are recording
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                // stop recording
                audioRecord.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            // Ensure we always unlock
            isCapturing = false;
            captureLock.unlock();
        }
        return true;
    }

    Runnable captureThread = new Runnable() {
        @Override
        public void run() {
            int samplesToRec = captureSamplingRate / 100;
            int samplesRead = 0;

            try {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception e) {
                // thread priority isn't fatal, just not 'great' so no failure exception thrown
                e.printStackTrace();
            }

            while (!shutdownCaptureThread) {
                captureLock.lock();
                try {
                    if (!CustomAudioDevice.this.isCapturing) {
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
                                    throw new RuntimeException("Audio Capture Error: Bad Value (-2)");
                                case AudioRecord.ERROR_INVALID_OPERATION:
                                    throw new RuntimeException("Audio Capture Error: Invalid Operation (-3)");
                                case AudioRecord.ERROR:
                                default:
                                    throw new RuntimeException("Audio Capture Error(-1)");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    return;
                } finally {
                    // Ensure we always unlock
                    captureLock.unlock();
                }
                getAudioBus().writeCaptureData(recBuffer, samplesRead);
                estimatedCaptureDelay = samplesRead * 1000 / captureSamplingRate;
            }
        }
    };

    @Override
    public boolean initRenderer() {
        // initalize default values
        bluetoothState = BluetoothState.DISCONNECTED;
        // initalize audio mode
        audioManagerMode.acquireMode(audioManager);
        // set default output routing
        setOutputMode(getOutputMode());
        /* register for bluetooth sco callbacks and attempt to enable it */
        enableBluetoothEvents();
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
        // check that the audioRecord is ready to be used
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new RuntimeException("Audio renderer not initialized " + rendererSettings.getSampleRate());
        }

        bufferedPlaySamples = 0;
        shutdownRenderThread = false;
        new Thread(renderThread).start();
        return true;
    }

    private void destroyAudioTrack() {
        rendererLock.lock();
        // release the object
        audioTrack.release();
        audioTrack = null;
        shutdownRenderThread = true;
        renderEvent.signal();
        rendererLock.unlock();
    }

    @Override
    public boolean destroyRenderer() {
        destroyAudioTrack();
        disableBluetoothEvents();
        unregisterHeadsetReceiver();
        audioManager.setSpeakerphoneOn(false);
        audioManagerMode.releaseMode(audioManager);
        return true;
    }

    public int getEstimatedRenderDelay() {
        return estimatedRenderDelay;
    }

    @Override
    public boolean startRenderer() {
        /* enable speakerphone unless headset is conencted */
        synchronized (bluetoothLock) {
            if (BluetoothState.CONNECTED != bluetoothState) {
                if (audioManager.isWiredHeadsetOn()) {
                    Log.d(LOG_TAG, "Turn off Speaker phone");
                    audioManager.setSpeakerphoneOn(false);
                } else {
                    Log.d(LOG_TAG, "Turn on Speaker phone");
                    audioManager.setSpeakerphoneOn(true);
                }
            }
        }
        // start playout
        try {
            audioTrack.play();
        } catch (IllegalStateException e) {
            throw new RuntimeException(e.getMessage());
        }
        rendererLock.lock();
        isRendering = true;
        renderEvent.signal();
        rendererLock.unlock();

        registerHeadsetReceiver();
        return true;
    }

    @Override
    public boolean stopRenderer() {
        rendererLock.lock();
        try {
            // only stop if we are playing
            if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                // stop playout
                audioTrack.stop();

            }
            // flush the buffers
            audioTrack.flush();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            // Ensure we always unlock, both for success, exception or error
            // return.
            isRendering = false;
            rendererLock.unlock();
        }
        unregisterHeadsetReceiver();
        return true;
    }

    Runnable renderThread = new Runnable() {

        @Override
        public void run() {
            int samplesToPlay = samplesPerBuffer;
            try {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception e) {
                // thread priority isn't fatal, just not 'great' so no failure exception thrown
                e.printStackTrace();
            }

            while (!shutdownRenderThread) {
                rendererLock.lock();
                try {
                    if (!CustomAudioDevice.this.isRendering) {
                        renderEvent.await();
                        continue;

                    } else {
                        rendererLock.unlock();

                        // Don't lock on audioBus calls
                        playBuffer.clear();
                        int samplesRead = getAudioBus().readRenderData(playBuffer, samplesToPlay);

                        // log.d("Samples read: " + samplesRead);

                        rendererLock.lock();

                        // After acquiring the lock again
                        // we must check if we are still playing
                        if (audioTrack == null || !CustomAudioDevice.this.isRendering) {
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
                                            "Audio Renderer Error: Bad Value (-2)");
                                case AudioTrack.ERROR_INVALID_OPERATION:
                                    throw new RuntimeException(
                                            "Audio Renderer Error: Invalid Operation (-3)");
                                case AudioTrack.ERROR:
                                default:
                                    throw new RuntimeException(
                                            "Audio Renderer Error(-1)");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    return;
                } finally {
                    rendererLock.unlock();
                }
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
        super.setOutputMode(mode);
        if (audioOutput != OutputType.BLUETOOTH && audioOutput != OutputType.HEADPHONES) {
            audioManager.setSpeakerphoneOn(mode == OutputMode.SpeakerPhone);
            return true;
        }
        return false;
    }

    private BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra(HEADSET_PLUG_STATE_KEY, 0) == 1) {
                    Log.d(LOG_TAG, "Headphones connected");
                    setOutputType(OutputType.HEADPHONES);
                    audioManager.setSpeakerphoneOn(false);
                    audioManager.setBluetoothScoOn(false);
                } else {
                    Log.d(LOG_TAG, "Headphones disconnected");
                    if (BluetoothState.CONNECTED == bluetoothState) {
                        audioManager.setBluetoothScoOn(true);
                        setOutputType(OutputType.BLUETOOTH);
                    } else {
                        audioManager.setSpeakerphoneOn(getOutputMode() == OutputMode.SpeakerPhone);
                        setOutputType(OutputType.PHONE_SPEAKERS);
                    }
                }
            }
        }
    };

    private boolean receiverRegistered;
    private boolean scoReceiverRegistered;

    private void registerHeadsetReceiver() {
        if (!receiverRegistered) {
            context.registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            receiverRegistered = true;
        }
    }

    private void unregisterHeadsetReceiver() {
        if (receiverRegistered) {
            context.unregisterReceiver(headsetReceiver);
            receiverRegistered = false;
        }
    }

    private void registerBtReceiver() {
        if (!scoReceiverRegistered) {
            context.registerReceiver(
                    btStatusReceiver,
                    new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            );
            scoReceiverRegistered = true;
        }
    }

    private void unregisterBtReceiver() {
        if (scoReceiverRegistered) {
            context.unregisterReceiver(btStatusReceiver);
            scoReceiverRegistered = false;
        }
    }

    @Override
    public synchronized void onPause() {
        if (isRendering && getOutputMode() == OutputMode.SpeakerPhone) {
            unregisterHeadsetReceiver();
        }
    }

    @Override
    public synchronized void onResume() {
        /* register handler for phonejack notifications */
        if (isRendering && getOutputMode() == OutputMode.SpeakerPhone) {
            registerHeadsetReceiver();
            if (!audioManager.isWiredHeadsetOn()) {
                audioManager.setSpeakerphoneOn(true);
            }
        }
        /* force reconnection of bluetooth in the event of a phone call */
        synchronized (bluetoothLock) {
            if (audioOutput == OutputType.BLUETOOTH) {
                bluetoothState = BluetoothState.DISCONNECTED;
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.getProfileProxy(
                            context,
                            bluetoothProfileListener,
                            BluetoothProfile.HEADSET

                    );
                }
            }
        }
    }

    private void enableBluetoothEvents() {
        if (audioManager.isBluetoothScoAvailableOffCall()) {
            registerBtReceiver();
            if (bluetoothAdapter != null) {
                bluetoothAdapter.getProfileProxy(
                        context,
                        bluetoothProfileListener,
                        BluetoothProfile.HEADSET
                );
            }
        }
    }

    private void disableBluetoothEvents() {
        if (null != bluetoothProfile && bluetoothAdapter != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothProfile);
        }
        unregisterBtReceiver();
        /* force a shutdown of bluetooth: when a call comes in, the handler is not invoked by system */
        Intent intent = new Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
        btStatusReceiver.onReceive(context, intent);
    }

    private void startBluetoothSco() {
        try {
            audioManager.startBluetoothSco();
        } catch (NullPointerException npex) {
            Log.d(LOG_TAG,
                    "Failed to start the BT SCO. In Android 5.0 calling "
                            + "[start|stop]BluetoothSco produces a NPE in some devices"
            );
        }
    }

    private void stopBluetoothSco() {
        try {
            audioManager.stopBluetoothSco();
        } catch (NullPointerException npex) {
            Log.d(LOG_TAG,
                    "Failed to start the BT SCO. In Android 5.0 calling "
                            + "[start|stop]BluetoothSco produces a NPE in some devices"
            );
        }
    }

    private void setOutputType(OutputType type) {
        audioOutput = type;
    }
}