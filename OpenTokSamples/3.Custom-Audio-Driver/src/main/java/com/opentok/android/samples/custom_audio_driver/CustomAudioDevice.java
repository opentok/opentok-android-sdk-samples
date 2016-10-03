package com.opentok.android.samples.custom_audio_driver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

import com.opentok.android.BaseAudioDevice;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomAudioDevice extends BaseAudioDevice {

    private final static String LOG_TAG = "opentok-defaultaudiodevice";

    private final static int SAMPLING_RATE = 44100;
    private final static int NUM_CHANNELS_CAPTURING = 1;
    private final static int NUM_CHANNELS_RENDERING = 1;

    private final static int MAX_SAMPLES = 2 * 480 * 2; // Max 10 ms @ 48 kHz

    private Context m_context;

    private AudioTrack m_audioTrack;
    private AudioRecord m_audioRecord;

    // Capture & render buffers
    private ByteBuffer m_playBuffer;
    private ByteBuffer m_recBuffer;
    private byte[] m_tempBufPlay;
    private byte[] m_tempBufRec;

    private final ReentrantLock m_rendererLock = new ReentrantLock(true);
    private final Condition m_renderEvent = m_rendererLock.newCondition();
    private volatile boolean m_isRendering = false;
    private volatile boolean m_shutdownRenderThread = false;

    private final ReentrantLock m_captureLock = new ReentrantLock(true);
    private final Condition m_captureEvent = m_captureLock.newCondition();
    private volatile boolean m_isCapturing = false;
    private volatile boolean m_shutdownCaptureThread = false;

    private AudioSettings m_captureSettings;
    private AudioSettings m_rendererSettings;

    // Capturing delay estimation
    private int m_estimatedCaptureDelay = 0;

    // Rendering delay estimation
    private int m_bufferedPlaySamples = 0;
    private int m_playPosition = 0;
    private int m_estimatedRenderDelay = 0;

    private AudioManager m_audioManager;

    public CustomAudioDevice(Context context) {
        this.m_context = context;

        try {
            m_playBuffer = ByteBuffer.allocateDirect(MAX_SAMPLES);
            m_recBuffer = ByteBuffer.allocateDirect(MAX_SAMPLES);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        m_tempBufPlay = new byte[MAX_SAMPLES];
        m_tempBufRec = new byte[MAX_SAMPLES];

        m_captureSettings = new AudioSettings(SAMPLING_RATE,
                NUM_CHANNELS_CAPTURING);
        m_rendererSettings = new AudioSettings(SAMPLING_RATE,
                NUM_CHANNELS_RENDERING);

        m_audioManager = (AudioManager) m_context
                .getSystemService(Context.AUDIO_SERVICE);

        m_audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    @Override
    public boolean initCapturer() {

        // get the minimum buffer size that can be used
        int minRecBufSize = AudioRecord.getMinBufferSize(m_captureSettings
                        .getSampleRate(),
                NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO
                        : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        // double size to be more safe
        int recBufSize = minRecBufSize * 2;

        // release the object
        if (m_audioRecord != null) {
            m_audioRecord.release();
            m_audioRecord = null;
        }

        try {
            m_audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
                    m_captureSettings.getSampleRate(),
                    NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO
                            : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, recBufSize);

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return false;
        }

        // check that the audioRecord is ready to be used
        if (m_audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.i(LOG_TAG, "Audio capture is not initialized "
                    + m_captureSettings.getSampleRate());

            return false;
        }

        m_shutdownCaptureThread = false;
        new Thread(m_captureThread).start();

        return true;
    }

    @Override
    public boolean destroyCapturer() {
        m_captureLock.lock();
        // release the object
        m_audioRecord.release();
        m_audioRecord = null;
        m_shutdownCaptureThread = true;
        m_captureEvent.signal();

        m_captureLock.unlock();
        return true;
    }

    public int getEstimatedCaptureDelay() {
        return m_estimatedCaptureDelay;
    }

    @Override
    public boolean startCapturer() {
        // start recording
        try {
            m_audioRecord.startRecording();

        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

        m_captureLock.lock();
        m_isCapturing = true;
        m_captureEvent.signal();
        m_captureLock.unlock();

        return true;
    }

    @Override
    public boolean stopCapturer() {
        m_captureLock.lock();
        try {
            // only stop if we are recording
            if (m_audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                // stop recording
                try {
                    m_audioRecord.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } finally {
            // Ensure we always unlock
            m_isCapturing = false;
            m_captureLock.unlock();
        }

        return true;
    }

    private Runnable m_captureThread = new Runnable() {
        @Override
        public void run() {

            int samplesToRec = SAMPLING_RATE / 100;
            int samplesRead = 0;

            try {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (!m_shutdownCaptureThread) {

                m_captureLock.lock();

                try {

                    if (!CustomAudioDevice.this.m_isCapturing) {
                        m_captureEvent.await();
                        continue;

                    } else {

                        if (m_audioRecord == null) {
                            continue;
                        }

                        int lengthInBytes = (samplesToRec << 1)
                                * NUM_CHANNELS_CAPTURING;
                        int readBytes = m_audioRecord.read(m_tempBufRec, 0,
                                lengthInBytes);

                        m_recBuffer.rewind();
                        m_recBuffer.put(m_tempBufRec);

                        samplesRead = (readBytes >> 1) / NUM_CHANNELS_CAPTURING;

                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "RecordAudio try failed: " + e.getMessage());
                    continue;

                } finally {
                    // Ensure we always unlock
                    m_captureLock.unlock();
                }

                getAudioBus().writeCaptureData(m_recBuffer, samplesRead);
                m_estimatedCaptureDelay = samplesRead * 1000 / SAMPLING_RATE;
            }
        }
    };

    @Override
    public boolean initRenderer() {

        // get the minimum buffer size that can be used
        int minPlayBufSize = AudioTrack.getMinBufferSize(m_rendererSettings
                        .getSampleRate(),
                NUM_CHANNELS_RENDERING == 1 ? AudioFormat.CHANNEL_OUT_MONO
                        : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        int playBufSize = minPlayBufSize;
        if (playBufSize < 6000) {
            playBufSize *= 2;
        }

        // release the object
        if (m_audioTrack != null) {
            m_audioTrack.release();
            m_audioTrack = null;
        }

        try {
            m_audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    m_rendererSettings.getSampleRate(),
                    NUM_CHANNELS_RENDERING == 1 ? AudioFormat.CHANNEL_OUT_MONO
                            : AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, playBufSize,
                    AudioTrack.MODE_STREAM);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return false;
        }

        // check that the audioRecord is ready to be used
        if (m_audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.i(LOG_TAG, "Audio renderer not initialized "
                    + m_rendererSettings.getSampleRate());
            return false;
        }

        m_bufferedPlaySamples = 0;

        setOutputMode(OutputMode.SpeakerPhone);

        m_shutdownRenderThread = false;
        new Thread(m_renderThread).start();

        return true;
    }

    @Override
    public boolean destroyRenderer() {
        m_rendererLock.lock();
        // release the object
        m_audioTrack.release();
        m_audioTrack = null;
        m_shutdownRenderThread = true;
        m_renderEvent.signal();
        m_rendererLock.unlock();

        unregisterHeadsetReceiver();
        m_audioManager.setSpeakerphoneOn(false);
        m_audioManager.setMode(AudioManager.MODE_NORMAL);

        return true;
    }

    public int getEstimatedRenderDelay() {
        return m_estimatedRenderDelay;
    }

    @Override
    public boolean startRenderer() {
        // start playout
        try {
            m_audioTrack.play();

        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

        m_rendererLock.lock();
        m_isRendering = true;
        m_renderEvent.signal();
        m_rendererLock.unlock();

        return true;
    }

    @Override
    public boolean stopRenderer() {
        m_rendererLock.lock();
        try {
            // only stop if we are playing
            if (m_audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                // stop playout
                try {
                    m_audioTrack.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    return false;
                }

                // flush the buffers
                m_audioTrack.flush();
            }

        } finally {
            // Ensure we always unlock, both for success, exception or error
            // return.
            m_isRendering = false;

            m_rendererLock.unlock();
        }

        return true;
    }

    private Runnable m_renderThread = new Runnable() {

        @Override
        public void run() {
            int samplesToPlay = SAMPLING_RATE / 100;

            try {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (!m_shutdownRenderThread) {
                m_rendererLock.lock();
                try {
                    if (!CustomAudioDevice.this.m_isRendering) {
                        m_renderEvent.await();
                        continue;

                    } else {
                        m_rendererLock.unlock();

                        // Don't lock on audioBus calls
                        m_playBuffer.clear();
                        int samplesRead = getAudioBus().readRenderData(
                                m_playBuffer, samplesToPlay);

                        // Log.d(LOG_TAG, "Samples read: " + samplesRead);

                        m_rendererLock.lock();

                        // After acquiring the lock again
                        // we must check if we are still playing
                        if (m_audioTrack == null
                                || !CustomAudioDevice.this.m_isRendering) {
                            continue;
                        }

                        int bytesRead = (samplesRead << 1)
                                * NUM_CHANNELS_RENDERING;
                        m_playBuffer.get(m_tempBufPlay, 0, bytesRead);

                        int bytesWritten = m_audioTrack.write(m_tempBufPlay, 0,
                                bytesRead);

                        // increase by number of written samples
                        m_bufferedPlaySamples += (bytesWritten >> 1)
                                / NUM_CHANNELS_RENDERING;

                        // decrease by number of played samples
                        int pos = m_audioTrack.getPlaybackHeadPosition();
                        if (pos < m_playPosition) {
                            // wrap or reset by driver
                            m_playPosition = 0;
                        }
                        m_bufferedPlaySamples -= (pos - m_playPosition);
                        m_playPosition = pos;

                        // we calculate the estimated delay based on the
                        // buffered samples
                        m_estimatedRenderDelay = m_bufferedPlaySamples * 1000
                                / SAMPLING_RATE;

                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    m_rendererLock.unlock();
                }
            }
        }
    };

    @Override
    public AudioSettings getCaptureSettings() {
        return this.m_captureSettings;
    }

    @Override
    public AudioSettings getRenderSettings() {
        return this.m_rendererSettings;
    }

    /**
     * Communication modes handling
     */

    public boolean setOutputMode(OutputMode mode) {
        super.setOutputMode(mode);
        if (mode == OutputMode.Handset) {
            unregisterHeadsetReceiver();
            m_audioManager.setSpeakerphoneOn(false);
        } else {
            m_audioManager.setSpeakerphoneOn(true);
            registerHeadsetReceiver();
        }
        return true;
    }

    private BroadcastReceiver m_headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().compareTo(Intent.ACTION_HEADSET_PLUG) == 0) {
                int state = intent.getIntExtra("state", 0);
                if (state == 0) {
                    m_audioManager.setSpeakerphoneOn(true);
                } else {
                    m_audioManager.setSpeakerphoneOn(false);
                }
            }
        }
    };

    private boolean m_receiverRegistered;

    private void registerHeadsetReceiver() {
        if (!m_receiverRegistered) {
            IntentFilter receiverFilter = new IntentFilter(
                    Intent.ACTION_HEADSET_PLUG);

            m_context.registerReceiver(m_headsetReceiver, receiverFilter);
            m_receiverRegistered = true;
        }
    }

    private void unregisterHeadsetReceiver() {
        if (m_receiverRegistered) {
            try {
                m_context.unregisterReceiver(m_headsetReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            m_receiverRegistered = false;
        }
    }

    @Override
    public void onPause() {
        if (getOutputMode() == OutputMode.SpeakerPhone) {
            unregisterHeadsetReceiver();
        }
    }

    @Override
    public void onResume() {
        if (getOutputMode() == OutputMode.SpeakerPhone) {
            registerHeadsetReceiver();
        }
    }
}