package com.tokbox.sample.livephotocapture

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Environment
import android.util.Log
import android.view.View
import com.opentok.android.BaseVideoRenderer
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.experimental.and

class ScreenshotVideoRenderer(var context: Context) : BaseVideoRenderer() {
    private var view = GLSurfaceView(context)
    private var renderer: MyRenderer

    class MyRenderer : GLSurfaceView.Renderer {
        private var textureIds = IntArray(3)
        private var scaleMatrix = FloatArray(16)
        private val vertexBuffer: FloatBuffer
        private val textureBuffer: FloatBuffer
        private val drawListBuffer: ShortBuffer
        private var videoFitEnabled = true
        private var videoDisabled = false
        private val vertexIndex = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw

        // vertices
        private val vertexShaderCode = """uniform mat4 uMVPMatrix;attribute vec4 aPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
  gl_Position = uMVPMatrix * aPosition;
  vTextureCoord = aTextureCoord;
}
"""
        private val fragmentShaderCode = """precision mediump float;
uniform sampler2D Ytex;
uniform sampler2D Utex,Vtex;
varying vec2 vTextureCoord;
void main(void) {
  float nx,ny,r,g,b,y,u,v;
  mediump vec4 txl,ux,vx;  nx=vTextureCoord[0];
  ny=vTextureCoord[1];
  y=texture2D(Ytex,vec2(nx,ny)).r;
  u=texture2D(Utex,vec2(nx,ny)).r;
  v=texture2D(Vtex,vec2(nx,ny)).r;
  y=1.1643*(y-0.0625);
  u=u-0.5;
  v=v-0.5;
  r=y+1.5958*v;
  g=y-0.39173*u-0.81290*v;
  b=y+2.017*u;
  gl_FragColor=vec4(r,g,b,1.0);
}
"""
        private var frameLock = ReentrantLock()
        private var currentFrame: Frame? = null
        private var program = 0
        private var textureWidth = 0
        private var textureHeight = 0
        private var viewportWidth = 0
        private var viewportHeight = 0
        private var saveScreenshot = false
        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            program = GLES20.glCreateProgram() // create empty OpenGL ES
            // Program
            GLES20.glAttachShader(program, vertexShader) // add the vertex
            // shader to program
            GLES20.glAttachShader(program, fragmentShader) // add the fragment
            // shader to
            // program
            GLES20.glLinkProgram(program)
            val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            val textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
            GLES20.glVertexAttribPointer(
                positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * 4,
                vertexBuffer
            )
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(
                textureHandle,
                TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                TEXTURE_COORDS_PER_VERTEX * 4, textureBuffer
            )
            GLES20.glEnableVertexAttribArray(textureHandle)
            GLES20.glUseProgram(program)
            var i = GLES20.glGetUniformLocation(program, "Ytex")
            GLES20.glUniform1i(i, 0) /* Bind Ytex to texture unit 0 */
            i = GLES20.glGetUniformLocation(program, "Utex")
            GLES20.glUniform1i(i, 1) /* Bind Utex to texture unit 1 */
            i = GLES20.glGetUniformLocation(program, "Vtex")
            GLES20.glUniform1i(i, 2) /* Bind Vtex to texture unit 2 */
            textureWidth = 0
            textureHeight = 0
        }

        private fun setupTextures(frame: Frame) {
            if (textureIds[0] != 0) {
                GLES20.glDeleteTextures(3, textureIds, 0)
            }
            GLES20.glGenTextures(3, textureIds, 0)
            val w = frame.width
            val h = frame.height
            val hw = w + 1 shr 1
            val hh = h + 1 shr 1
            initializeTexture(GLES20.GL_TEXTURE0, textureIds[0], w, h)
            initializeTexture(GLES20.GL_TEXTURE1, textureIds[1], hw, hh)
            initializeTexture(GLES20.GL_TEXTURE2, textureIds[2], hw, hh)
            textureWidth = frame.width
            textureHeight = frame.height
        }

        private fun updateTextures(frame: Frame) {
            val width = frame.width
            val height = frame.height
            val half_width = width + 1 shr 1
            val half_height = height + 1 shr 1
            val y_size = width * height
            val uv_size = half_width * half_height
            val bb = frame.buffer
            // If we are reusing this frame, make sure we reset position and
            // limit
            bb.clear()
            if (bb.remaining() == y_size + uv_size * 2) {
                bb.position(0)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                    bb
                )
                bb.position(y_size)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1])
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    half_width, half_height, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, bb
                )
                bb.position(y_size + uv_size)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[2])
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    half_width, half_height, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, bb
                )
            } else {
                textureWidth = 0
                textureHeight = 0
            }
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            viewportWidth = width
            viewportHeight = height
        }

        override fun onDrawFrame(gl: GL10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            frameLock.lock()
            if (currentFrame != null && !videoDisabled) {
                GLES20.glUseProgram(program)
                if (textureWidth != currentFrame!!.width
                    || textureHeight != currentFrame!!.height
                ) {
                    setupTextures(currentFrame!!)
                }
                updateTextures(currentFrame!!)
                Matrix.setIdentityM(scaleMatrix, 0)
                var scaleX = 1.0f
                var scaleY = 1.0f
                val ratio = (currentFrame!!.width.toFloat()
                    / currentFrame!!.height)
                val vratio = viewportWidth.toFloat() / viewportHeight

                if (videoFitEnabled) {
                    if (ratio > vratio) {
                        scaleY = vratio / ratio
                    } else {
                        scaleX = ratio / vratio
                    }
                } else {
                    if (ratio < vratio) {
                        scaleY = vratio / ratio
                    } else {
                        scaleX = ratio / vratio
                    }
                }

                Matrix.scaleM(
                    scaleMatrix, 0,
                    scaleX * if (currentFrame!!.isMirroredX) -1.0f else 1.0f,
                    scaleY, 1f
                )
                val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, scaleMatrix, 0)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, vertexIndex.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
            }

            frameLock.unlock()
        }

        fun displayFrame(frame: Frame) {
            frameLock.lock()

            if (currentFrame != null) {
                currentFrame!!.destroy() // Disposes previous frame
            }

            currentFrame = frame
            frameLock.unlock()

            if (saveScreenshot) {
                Log.d(TAG, "Screenshot capture")
                val bb = frame.buffer
                bb.clear()
                val width = frame.width
                val height = frame.height
                val half_width = width + 1 shr 1
                val half_height = height + 1 shr 1
                val y_size = width * height
                val uv_size = half_width * half_height
                val yuv = ByteArray(y_size + uv_size * 2)
                bb[yuv]
                val intArray = IntArray(width * height)

                // Decode Yuv data to integer array
                decodeYUV420(intArray, yuv, width, height)

                // Initialize the bitmap, with the replaced color
                val bmp = Bitmap.createBitmap(intArray, width, height, Bitmap.Config.ARGB_8888)
                try {
                    val path = Environment.getExternalStorageDirectory().toString()
                    var fOutputStream: OutputStream? = null
                    val file = File(path, "opentok-capture-${System.currentTimeMillis()}.png")
                    fOutputStream = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream)
                    fOutputStream.flush()
                    fOutputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
                saveScreenshot = false
            }
        }

        fun disableVideo(b: Boolean) {
            frameLock.lock()
            videoDisabled = b
            if (videoDisabled) {
                currentFrame = null
            }
            frameLock.unlock()
        }

        fun enableVideoFit(enableVideoFit: Boolean) {
            videoFitEnabled = enableVideoFit
        }

        fun saveScreenshot() {
            saveScreenshot = true
        }

        companion object {
            // number of coordinates per vertex in this array
            const val COORDS_PER_VERTEX = 3
            const val TEXTURE_COORDS_PER_VERTEX = 2
            var xyzCoords = floatArrayOf(
                -1.0f, 1.0f, 0.0f,  // top left
                -1.0f, -1.0f, 0.0f,  // bottom left
                1.0f, -1.0f, 0.0f,  // bottom right
                1.0f, 1.0f, 0.0f // top right
            )
            var uvCoords = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f) // top right
            fun initializeTexture(name: Int, id: Int, width: Int, height: Int) {
                GLES20.glActiveTexture(name)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)

                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat()
                )

                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
                )

                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )

                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )

                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                    width, height, 0, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, null
                )
            }

            fun decodeYUV420(rgba: IntArray, yuv420: ByteArray, width: Int, height: Int) {
                val halfWidth = width + 1 shr 1
                val halfHeight = height + 1 shr 1
                val ySize = width * height
                val uvSize = halfWidth * halfHeight

                for (j in 0 until height) {
                    for (i in 0 until width) {
                        val y: Double = (yuv420[j * width + i] and 1).toDouble()
                        val v: Double = (yuv420[ySize + (j shr 1) * halfWidth + (i shr 1)] and 1).toDouble()
                        val u: Double =
                            (yuv420[ySize + uvSize + (j shr 1) * halfWidth + (i shr 1)] and 1).toDouble()
                        var r: Double
                        var g: Double
                        var b: Double
                        r = y + 1.402 * (u - 128)
                        g = y - 0.34414 * (v - 128) - 0.71414 * (u - 128)
                        b = y + 1.772 * (v - 128)
                        if (r < 0) r = 0.0 else if (r > 255) r = 255.0
                        if (g < 0) g = 0.0 else if (g > 255) g = 255.0
                        if (b < 0) b = 0.0 else if (b > 255) b = 255.0
                        val ir = r.toInt()
                        val ig = g.toInt()
                        val ib = b.toInt()
                        rgba[j * width + i] = -0x1000000 or (ir shl 16) or (ig shl 8) or ib
                    }
                }
            }

            fun loadShader(type: Int, shaderCode: String?): Int {
                val shader = GLES20.glCreateShader(type)
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                return shader
            }
        }

        init {
            val bb = ByteBuffer.allocateDirect(xyzCoords.size * 4)
            bb.order(ByteOrder.nativeOrder())
            vertexBuffer = bb.asFloatBuffer()
            vertexBuffer.put(xyzCoords)
            vertexBuffer.position(0)

            val tb = ByteBuffer.allocateDirect(uvCoords.size * 4)
            tb.order(ByteOrder.nativeOrder())
            textureBuffer = tb.asFloatBuffer()
            textureBuffer.put(uvCoords)
            textureBuffer.position(0)

            val dlb = ByteBuffer.allocateDirect(vertexIndex.size * 2)
            dlb.order(ByteOrder.nativeOrder())
            drawListBuffer = dlb.asShortBuffer()
            drawListBuffer.put(vertexIndex)
            drawListBuffer.position(0)
        }
    }

    override fun onFrame(frame: Frame) {
        renderer.displayFrame(frame)
        view.requestRender()
    }

    override fun setStyle(key: String, value: String) {
        if (STYLE_VIDEO_SCALE == key) {
            if (STYLE_VIDEO_FIT == value) {
                renderer.enableVideoFit(true)
            } else if (STYLE_VIDEO_FILL == value) {
                renderer.enableVideoFit(false)
            }
        }
    }

    override fun onVideoPropertiesChanged(videoEnabled: Boolean) {
        renderer.disableVideo(!videoEnabled)
    }

    override fun getView(): View {
        return view
    }

    override fun onPause() {
        view.onPause()
    }

    override fun onResume() {
        view.onResume()
    }

    fun saveScreenshot() {
        renderer.saveScreenshot()
    }

    companion object {
        private val TAG = ScreenshotVideoRenderer::class.java.simpleName
    }

    init {
        view.setEGLContextClientVersion(2)
        renderer = MyRenderer()
        view.setRenderer(renderer)
        view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
}