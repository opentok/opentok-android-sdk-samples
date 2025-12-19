package com.tokbox.sample.webviewscreensharing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.view.View
import com.opentok.android.BaseVideoCapturer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenSharingCapturer(private val context: Context, private val contentView: View) :
    BaseVideoCapturer() {
    private val capturing = AtomicBoolean(false)
    private val fps = 15
    private var width = 0
    private var height = 0
    private var frame: IntArray? = null
    private var bmp: Bitmap? = null
    private var canvas: Canvas? = null
    private val handler = Handler()
    private val newFrame: Runnable = object : Runnable {
        override fun run() {
            if (capturing.get()) {
                val width = contentView.width
                val height = contentView.height
                if (frame == null || this@ScreenSharingCapturer.width != width || this@ScreenSharingCapturer.height != height) {
                    this@ScreenSharingCapturer.width = width
                    this@ScreenSharingCapturer.height = height
                    if (bmp != null) {
                        bmp!!.recycle()
                        bmp = null
                    }
                    bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    canvas = Canvas(bmp!!)
                    frame = IntArray(width * height)
                }

                val localCanvas = canvas
                if (localCanvas != null) {
                    localCanvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
                    localCanvas.translate(-contentView.scrollX.toFloat(), -contentView.scrollY.toFloat())
                    contentView.draw(localCanvas)
                    val localFrame = frame
                    if(localFrame != null){
                        bmp!!.getPixels(localFrame, 0, width, 0, 0, width, height)
                        provideIntArrayFrame(frame, ARGB, width, height, 0, false)
                        localCanvas.restore()
                        handler.postDelayed(this, (1000 / fps).toLong())
                    }
                }

            }
        }
    }

    override fun init() {}
    override fun startCapture(): Int {
        if (capturing.compareAndSet(false, true)) {
            // Start capturing
            handler.postDelayed(newFrame, (1000 / fps).toLong())
            return 0
        } else {
            // Already capturing
            return -1
        }
    }

    override fun stopCapture(): Int {
        if (capturing.compareAndSet(true, false)) {
            // Stop capturing
            handler.removeCallbacks(newFrame)
            return 0
        } else {
            // Not capturing
            return -1
        }
    }

    override fun isCaptureStarted(): Boolean {
        return capturing.get()
    }

    override fun getCaptureSettings(): CaptureSettings {
        val captureSettings = CaptureSettings()
        captureSettings.fps = fps
        captureSettings.width = width
        captureSettings.height = height
        captureSettings.format = ARGB
        return captureSettings
    }

    override fun destroy() {}
    override fun onPause() {}
    override fun onResume() {}
}