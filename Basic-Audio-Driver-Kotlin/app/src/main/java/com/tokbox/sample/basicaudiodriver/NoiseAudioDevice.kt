package com.tokbox.sample.basicaudiodriver

import android.content.Context
import android.os.Environment
import android.os.Handler
import com.opentok.android.BaseAudioDevice
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Random

class NoiseAudioDevice(private val context: Context) : BaseAudioDevice() {
    private val captureSettings: AudioSettings
    private val rendererSettings: AudioSettings
    private var capturerStarted: Boolean = false
    private var rendererStarted: Boolean = false
    private var audioDriverPaused: Boolean = false
    private var capturerBuffer: ByteBuffer? = null
    private var rendererBuffer: ByteBuffer? = null
    private var rendererFile: File? = null
    private var capturerHandler: Handler? = null
    private val capturerIntervalMillis: Long = 1000

    private val capturer: Runnable = object : Runnable {
        override fun run() {
            capturerBuffer?.rewind()
            val rand = Random()
            rand.nextBytes(capturerBuffer?.array())
            audioBus.writeCaptureData(capturerBuffer, SAMPLING_RATE)
            if (capturerStarted && !audioDriverPaused) {
                capturerHandler?.postDelayed(this, capturerIntervalMillis)
            }
        }
    }
    private val rendererIntervalMillis: Long = 1000
    private var rendererHandler: Handler? = null

    private val renderer: Runnable = object : Runnable {
        override fun run() {
            rendererBuffer?.clear()
            audioBus.readRenderData(rendererBuffer, SAMPLING_RATE)
            try {
                val stream = FileOutputStream(rendererFile)
                stream.write(rendererBuffer?.array())
                stream.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (rendererStarted && !audioDriverPaused) {
                rendererHandler?.postDelayed(this, rendererIntervalMillis)
            }
        }
    }

    override fun initCapturer(): Boolean {
        capturerBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2) // Each sample has 2 bytes
        return true
    }

    override fun startCapturer(): Boolean {
        capturerStarted = true
        capturerHandler?.postDelayed(capturer, capturerIntervalMillis)
        return true
    }

    override fun stopCapturer(): Boolean {
        capturerStarted = false
        capturerHandler?.removeCallbacks(capturer)
        return true
    }

    override fun destroyCapturer(): Boolean {
        return true
    }

    override fun initRenderer(): Boolean {
        rendererBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2) // Each sample has 2 bytes
        val documentsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        rendererFile = File(documentsDirectory, "output.raw")
        if (rendererFile?.exists() == false) {
            try {
                rendererFile?.parentFile?.mkdirs()
                rendererFile?.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return true
    }

    override fun startRenderer(): Boolean {
        rendererStarted = true
        rendererHandler?.postDelayed(renderer, rendererIntervalMillis)
        return true
    }

    override fun stopRenderer(): Boolean {
        rendererStarted = false
        return true
    }

    override fun destroyRenderer(): Boolean {
        return true
    }

    override fun getEstimatedCaptureDelay(): Int {
        return 0
    }

    override fun getEstimatedRenderDelay(): Int {
        return 0
    }

    override fun getCaptureSettings(): AudioSettings {
        return captureSettings
    }

    override fun getRenderSettings(): AudioSettings {
        return rendererSettings
    }

    override fun onPause() {
        audioDriverPaused = true
        capturerHandler?.removeCallbacks(capturer)
        rendererHandler?.removeCallbacks(renderer)
    }

    override fun onResume() {
        audioDriverPaused = false
        if (capturerStarted) {
            capturerHandler?.postDelayed(capturer, capturerIntervalMillis)
        }
        if (rendererStarted) {
            rendererHandler?.postDelayed(renderer, rendererIntervalMillis)
        }
    }

    companion object {
        private const val SAMPLING_RATE = 44100
        private const val NUM_CHANNELS_CAPTURING = 1
        private const val NUM_CHANNELS_RENDERING = 1
    }

    init {
        captureSettings = AudioSettings(SAMPLING_RATE, NUM_CHANNELS_CAPTURING)
        rendererSettings = AudioSettings(SAMPLING_RATE, NUM_CHANNELS_RENDERING)
        capturerStarted = false
        rendererStarted = false
        audioDriverPaused = false
        capturerHandler = Handler()
        rendererHandler = Handler()
    }
}