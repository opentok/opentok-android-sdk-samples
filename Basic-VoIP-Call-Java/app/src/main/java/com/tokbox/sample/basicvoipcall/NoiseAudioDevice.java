package com.tokbox.sample.basicvoipcall;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import com.opentok.android.BaseAudioDevice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

public class NoiseAudioDevice extends BaseAudioDevice {
    private final static int SAMPLING_RATE = 44100;
    private final static int NUM_CHANNELS_CAPTURING = 1;
    private final static int NUM_CHANNELS_RENDERING = 1;

    private Context context;

    private AudioSettings captureSettings;
    private AudioSettings rendererSettings;

    private boolean capturerStarted;
    private boolean rendererStarted;

    private boolean audioDriverPaused;

    private ByteBuffer capturerBuffer;
    private ByteBuffer rendererBuffer;
    private File rendererFile;

    private Handler capturerHandler;
    private long capturerIntervalMillis = 1000;

    private Runnable capturer = new Runnable() {
        @Override
        public void run() {
            capturerBuffer.rewind();

            Random rand = new Random();
            rand.nextBytes(capturerBuffer.array());

            getAudioBus().writeCaptureData(capturerBuffer, SAMPLING_RATE);

            if(capturerStarted && !audioDriverPaused) {
                capturerHandler.postDelayed(capturer, capturerIntervalMillis);
            }
        }
    };

    private long rendererIntervalMillis = 1000;
    private Handler rendererHandler;

    private Runnable renderer = new Runnable() {
        @Override
        public void run() {
            rendererBuffer.clear();
            getAudioBus().readRenderData(rendererBuffer, SAMPLING_RATE);

            try {
                FileOutputStream stream = new FileOutputStream(rendererFile);
                stream.write(rendererBuffer.array());
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (rendererStarted && !audioDriverPaused) {
                rendererHandler.postDelayed(renderer, rendererIntervalMillis);
            }
        }
    };

    public NoiseAudioDevice(Context context) {
        this.context = context;

        captureSettings = new AudioSettings(SAMPLING_RATE, NUM_CHANNELS_CAPTURING);
        rendererSettings = new AudioSettings(SAMPLING_RATE, NUM_CHANNELS_RENDERING);

        capturerStarted = false;
        rendererStarted = false;

        audioDriverPaused = false;

        capturerHandler = new Handler();
        rendererHandler = new Handler();
    }

    @Override
    public boolean initCapturer() {
        capturerBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2); // Each sample has 2 bytes
        return true;
    }

    @Override
    public boolean startCapturer() {
        capturerStarted = true;
        capturerHandler.postDelayed(capturer, capturerIntervalMillis);
        return true;
    }

    @Override
    public boolean stopCapturer() {
        capturerStarted = false;
        capturerHandler.removeCallbacks(capturer);
        return true;
    }

    @Override
    public boolean destroyCapturer() {
        return true;
    }

    @Override
    public boolean initRenderer() {
        rendererBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2); // Each sample has 2 bytes
        File documentsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        rendererFile = new File(documentsDirectory, "output.raw");

        if (!rendererFile.exists()) {
            try {
                rendererFile.getParentFile().mkdirs();
                rendererFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    @Override
    public boolean startRenderer() {
        rendererStarted = true;
        rendererHandler.postDelayed(renderer, rendererIntervalMillis);
        return true;
    }

    @Override
    public boolean stopRenderer() {
        rendererStarted = false;
        return true;
    }

    @Override
    public boolean destroyRenderer() {
        return true;
    }

    @Override
    public int getEstimatedCaptureDelay() {
        return 0;
    }

    @Override
    public int getEstimatedRenderDelay() {
        return 0;
    }

    @Override
    public AudioSettings getCaptureSettings() {
        return captureSettings;
    }

    @Override
    public AudioSettings getRenderSettings() {
        return rendererSettings;
    }

    @Override
    public void onPause() {
        audioDriverPaused = true;
        capturerHandler.removeCallbacks(capturer);
        rendererHandler.removeCallbacks(renderer);
    }

    @Override
    public void onResume() {
        audioDriverPaused = false;

        if (capturerStarted) {
            capturerHandler.postDelayed(capturer, capturerIntervalMillis);
        }

        if (rendererStarted) {
            rendererHandler.postDelayed(renderer, rendererIntervalMillis);
        }
    }
}
