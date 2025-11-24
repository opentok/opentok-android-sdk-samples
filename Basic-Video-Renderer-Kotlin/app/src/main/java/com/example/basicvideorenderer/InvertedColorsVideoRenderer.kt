package com.example.basicvideorenderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.View
import com.opentok.android.BaseVideoRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class InvertedColorsVideoRenderer(private val context: Context) : BaseVideoRenderer() {

    private val view: GLSurfaceView
    private val renderer: MyRenderer

    interface InvertedColorsVideoRendererMetadataListener {
        fun onMetadataReady(metadata: ByteArray)
    }

    private class MyRenderer : GLSurfaceView.Renderer {

        private val textureIds = IntArray(3)
        private val scaleMatrix = FloatArray(16)

        private lateinit var vertexBuffer: FloatBuffer
        private lateinit var textureBuffer: FloatBuffer
        private lateinit var drawListBuffer: ShortBuffer

        var videoFitEnabled = true
        var videoDisabled = false

        // number of coordinates per vertex in this array
        companion object {
            const val COORDS_PER_VERTEX = 3
            const val TEXTURE_COORDS_PER_VERTEX = 2

            val xyzCoords = floatArrayOf(
                -1.0f, 1.0f, 0.0f, // top left
                -1.0f, -1.0f, 0.0f, // bottom left
                1.0f, -1.0f, 0.0f, // bottom right
                1.0f, 1.0f, 0.0f // top right
            )

            val uvCoords = floatArrayOf(
                0f, 0f, // top left
                0f, 1f, // bottom left
                1f, 1f, // bottom right
                1f, 0f
            ) // top right

            val vertexIndex = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

            private const val vertexShaderCode = "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = aTextureCoord;\n" +
                    "}\n"

            private const val fragmentShaderCode = "precision mediump float;\n" +
                    "uniform sampler2D Ytex;\n" +
                    "uniform sampler2D Utex,Vtex;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main(void) {\n" +
                    "  float nx,ny,r,g,b,y,u,v;\n" +
                    "  mediump vec4 txl,ux,vx;" +
                    "  nx=vTextureCoord[0];\n" +
                    "  ny=vTextureCoord[1];\n" +
                    "  y=texture2D(Ytex,vec2(nx,ny)).r;\n" +
                    "  u=texture2D(Utex,vec2(nx,ny)).r;\n" +
                    "  v=texture2D(Vtex,vec2(nx,ny)).r;\n" +
                    "  y=1.0-1.1643*(y-0.0625);\n" + // this line produces the inverted effect
                    //   + "  y=1.1643*(y-0.0625);\n"  // use this line instead if you want to have normal colors
                    "  u=u-0.5;\n" +
                    "  v=v-0.5;\n" +
                    "  r=y+1.5958*v;\n" +
                    "  g=y-0.39173*u-0.81290*v;\n" +
                    "  b=y+2.017*u;\n" +
                    "  gl_FragColor=vec4(r,g,b,1.0);\n" +
                    "}\n"

            fun initializeTexture(name: Int, id: Int, width: Int, height: Int) {
                GLES20.glActiveTexture(name)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )
                GLES20.glTexParameterf(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE.toFloat()
                )

                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    GLES20.GL_LUMINANCE,
                    width,
                    height,
                    0,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    null
                )
            }

            fun loadShader(type: Int, shaderCode: String): Int {
                val shader = GLES20.glCreateShader(type)

                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)

                return shader
            }
        }

        private val frameLock = ReentrantLock()
        private var currentFrame: Frame? = null

        private var program = 0
        private var textureWidth = 0
        private var textureHeight = 0
        private var viewportWidth = 0
        private var viewportHeight = 0

        var metadataListener: InvertedColorsVideoRendererMetadataListener? = null

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

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            gl?.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

            program = GLES20.glCreateProgram() // create empty OpenGL ES Program
            GLES20.glAttachShader(program, vertexShader) // add the vertex shader to program
            GLES20.glAttachShader(program, fragmentShader) // add the fragment shader to program
            GLES20.glLinkProgram(program)

            val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")

            val textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")

            GLES20.glVertexAttribPointer(
                positionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                COORDS_PER_VERTEX * 4,
                vertexBuffer
            )

            GLES20.glEnableVertexAttribArray(positionHandle)

            GLES20.glVertexAttribPointer(
                textureHandle,
                TEXTURE_COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                TEXTURE_COORDS_PER_VERTEX * 4,
                textureBuffer
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
            val hw = (w + 1) shr 1
            val hh = (h + 1) shr 1

            initializeTexture(GLES20.GL_TEXTURE0, textureIds[0], w, h)
            initializeTexture(GLES20.GL_TEXTURE1, textureIds[1], hw, hh)
            initializeTexture(GLES20.GL_TEXTURE2, textureIds[2], hw, hh)

            textureWidth = frame.width
            textureHeight = frame.height
        }

        private fun glTexSubImage2D(width: Int, height: Int, stride: Int, buf: ByteBuffer) {
            if (stride == width) {
                // Yay!  We can upload the entire plane in a single GL call.
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    width,
                    height,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    buf
                )
            } else {
                for (row in 0 until height) {
                    buf.position(row * stride)
                    GLES20.glTexSubImage2D(
                        GLES20.GL_TEXTURE_2D,
                        0,
                        0,
                        row,
                        width,
                        1,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        buf
                    )
                }
            }
        }

        private fun updateTextures(frame: Frame) {
            val width = frame.width
            val height = frame.height
            val halfWidth = (width + 1) shr 1
            val halfHeight = (height + 1) shr 1

            val bb = frame.buffer
            bb.clear()
            //check if buffer data is correctly sized.
            if (bb.remaining() != frame.yplaneSize + frame.uVplaneSize * 2) {
                textureWidth = 0
                textureHeight = 0
                return
            }
            bb.position(0)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            glTexSubImage2D(width, height, frame.ystride, frame.yplane)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1])
            glTexSubImage2D(halfWidth, halfHeight, frame.uvStride, frame.uplane)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[2])
            glTexSubImage2D(halfWidth, halfHeight, frame.uvStride, frame.vplane)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            viewportWidth = width
            viewportHeight = height
        }

        override fun onDrawFrame(gl: GL10?) {
            gl?.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            frameLock.lock()
            if (currentFrame != null && !videoDisabled) {
                GLES20.glUseProgram(program)

                if (textureWidth != currentFrame!!.width || textureHeight != currentFrame!!.height) {
                    setupTextures(currentFrame!!)
                }

                updateTextures(currentFrame!!)

                Matrix.setIdentityM(scaleMatrix, 0)
                var scaleX = 1.0f
                var scaleY = 1.0f
                val ratio = currentFrame!!.width.toFloat() / currentFrame!!.height.toFloat()
                val vratio = viewportWidth.toFloat() / viewportHeight.toFloat()

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
                    scaleMatrix,
                    0,
                    scaleX * (if (currentFrame!!.isMirroredX) -1.0f else 1.0f),
                    scaleY,
                    1f
                )

                metadataListener?.onMetadataReady(currentFrame!!.metadata)

                val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, scaleMatrix, 0)

                GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES,
                    vertexIndex.size,
                    GLES20.GL_UNSIGNED_SHORT,
                    drawListBuffer
                )
            } else {
                //black frame when video is disabled
                gl?.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
            frameLock.unlock()
        }

        fun displayFrame(frame: Frame) {
            frameLock.lock()

            currentFrame?.destroy() // Disposes previous frame

            currentFrame = frame
            frameLock.unlock()
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
    }

    init {
        view = GLSurfaceView(context)
        view.setEGLContextClientVersion(2)

        renderer = MyRenderer()
        view.setRenderer(renderer)

        view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onFrame(frame: Frame) {
        renderer.displayFrame(frame)
        view.requestRender()
    }

    override fun setStyle(key: String, value: String) {
        if (BaseVideoRenderer.STYLE_VIDEO_SCALE == key) {
            when (value) {
                BaseVideoRenderer.STYLE_VIDEO_FIT -> renderer.enableVideoFit(true)
                BaseVideoRenderer.STYLE_VIDEO_FILL -> renderer.enableVideoFit(false)
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
}

