package com.tokbox.sample.videotransformers

import android.Manifest
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.opentok.android.*
import com.opentok.android.PublisherKit.*
import com.opentok.android.Session.SessionListener
import com.opentok.android.SubscriberKit.SubscriberListener
import com.tokbox.sample.videotransformers.MainActivity
import com.tokbox.sample.videotransformers.network.APIService
import com.tokbox.sample.videotransformers.network.GetSessionResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private var retrofit: Retrofit? = null
    private var apiService: APIService? = null
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null
    private lateinit var publisherViewContainer: FrameLayout
    private lateinit var subscriberViewContainer: FrameLayout

    private lateinit var logoTransformer: logoWatermark

    //Button to toggle Video Transformers
    private var buttonVideoTransformers: Button? = null

    // Array of Media Transformers
    var videoTransformers: ArrayList<VideoTransformer> = ArrayList()
    var audioTransformers: ArrayList<AudioTransformer> = ArrayList()

    private val publisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream ${stream.streamId}")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream ${stream.streamId}")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit onError: ${opentokError.message}")
        }
    }
    private val sessionListener: SessionListener = object : SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "onConnected: Connected to session: ${session.sessionId}")
            publisher = Publisher.Builder(this@MainActivity).build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.renderer?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            publisherViewContainer.addView(publisher?.view)
            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }
            session.publish(publisher)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: ${session.sessionId}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received ${stream.streamId} in session: ${session.sessionId}")
            if (subscriber == null) {
                subscriber = Subscriber.Builder(this@MainActivity, stream).build().also {
                    it.renderer?.setStyle(
                        BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL
                    )

                    it.setSubscriberListener(subscriberListener)
                }

                session.subscribe(subscriber)
                subscriberViewContainer.addView(subscriber?.view)
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: ${stream.streamId} in session: ${session.sessionId}")
            if (subscriber != null) {
                subscriber = null
                subscriberViewContainer.removeAllViews()
            }
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }
    }
    var subscriberListener: SubscriberListener = object : SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "onConnected: Subscriber connected. Stream: ${subscriberKit.stream.streamId}")
        }

        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "onDisconnected: Subscriber disconnected. Stream: ${subscriberKit.stream.streamId}")
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            finishWithMessage("SubscriberKit onError: ${opentokError.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        publisherViewContainer = findViewById(R.id.publisher_container)
        subscriberViewContainer = findViewById(R.id.subscriber_container)
        requestPermissions()

        buttonVideoTransformers = findViewById(R.id.setvideotransformers)

        // Initialize the logoTransformer after the MainActivity is fully initialized.
        logoTransformer = logoWatermark(resources)
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d(TAG, "onPermissionsGranted:$requestCode: $perms")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        finishWithMessage("onPermissionsDenied: $requestCode: $perms")
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            if (ServerConfig.hasChatServerUrl()) {
                // Custom server URL exists - retrieve session config
                if (!ServerConfig.isValid) {
                    finishWithMessage("Invalid chat server url: ${ServerConfig.CHAT_SERVER_URL}")
                    return
                }
                initRetrofit()
                getSession()
            } else {
                // Use hardcoded session config
                if (!OpenTokConfig.isValid) {
                    finishWithMessage("Invalid OpenTokConfig. ${OpenTokConfig.description}")
                    return
                }
                initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN)
            }
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_video_app),
                PERMISSIONS_REQUEST_CODE,
                *perms
            )
        }
    }

    /* Make a request for session data */
    private fun getSession() {
        Log.i(TAG, "getSession")

        apiService?.session?.enqueue(object : Callback<GetSessionResponse?> {
            override fun onResponse(call: Call<GetSessionResponse?>, response: Response<GetSessionResponse?>) {
                response.body()?.also {
                    initializeSession(it.apiKey, it.sessionId, it.token)
                }
            }

            override fun onFailure(call: Call<GetSessionResponse?>, t: Throwable) {
                throw RuntimeException(t.message)
            }
        })
    }

    private fun initializeSession(apiKey: String, sessionId: String, token: String) {
        Log.i(TAG, "apiKey: $apiKey")
        Log.i(TAG, "sessionId: $sessionId")
        Log.i(TAG, "token: $token")

        /*
        The context used depends on the specific use case, but usually, it is desired for the session to
        live outside of the Activity e.g: live between activities. For a production applications,
        it's convenient to use Application context instead of Activity context.
         */
        session = Session.Builder(this, apiKey, sessionId).build().also {
            it.setSessionListener(sessionListener)
            it.connect(token)
        }
    }

    private fun initRetrofit() {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(ServerConfig.CHAT_SERVER_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build().also {
                apiService = it.create(APIService::class.java)
            }
    }

    private fun finishWithMessage(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSIONS_REQUEST_CODE = 124
    }

    class logoWatermark(private val resources: Resources) : CustomVideoTransformer {

        // Get the image in bitmap format
        private val image: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.vonage_logo)

        fun resizeImage(image: Bitmap?, width: Int, height: Int): Bitmap {
            return Bitmap.createScaledBitmap(image!!, width, height, true)
        }

        override fun onTransform(frame: BaseVideoRenderer.Frame) {

            // Obtain the Y  plane of the video frame
            val yPlane: ByteBuffer = frame.yplane

            // Get the dimensions of the video frame
            val videoWidth = frame.width
            val videoHeight = frame.height

            // Calculate the desired size of the image
            val desiredWidth = videoWidth / 8 // Adjust this value as needed
            val desiredHeight: Int = (image.getHeight() * (desiredWidth.toFloat() / image.getWidth())).toInt()

            // Resize the image to the desired size
            val resizedImage = resizeImage(image, desiredWidth, desiredHeight)
            val logoWidth: Int = resizedImage.getWidth()
            val logoHeight: Int = resizedImage.getHeight()

            // Location of the image (center of video)
            val logoPositionX = videoWidth * 1 / 2 - logoWidth // Adjust this as needed for the desired position
            val logoPositionY = videoHeight * 1 / 2 - logoHeight // Adjust this as needed for the desired position

            // Overlay the logo on the video frame
            for (y in 0 until logoHeight) {
                for (x in 0 until logoWidth) {
                    val frameOffset = (logoPositionY + y) * videoWidth + (logoPositionX + x)

                    // Get the logo pixel color
                    val logoPixel: Int = resizedImage.getPixel(x, y)

                    // Extract the color channels (ARGB)
                    val logoAlpha = logoPixel shr 24 and 0xFF
                    val logoRed = logoPixel shr 16 and 0xFF

                    // Overlay the logo pixel on the video frame
                    val framePixel: Int = yPlane.get(frameOffset).toInt() and 0xFF

                    // Calculate the blended pixel value
                    val blendedPixel = (logoAlpha * logoRed + (255 - logoAlpha) * framePixel) / 255 and 0xFF

                    // Set the blended pixel value in the video frame
                    yPlane.put(frameOffset, blendedPixel.toByte())
                }
            }
        }
    }

    private var isSet = false
    fun setVideoTransformers(view: View?) {
        if (!isSet) {
            videoTransformers.clear()
            val backgroundBlur = publisher!!.VideoTransformer("BackgroundBlur", "{\"radius\":\"High\"}")
            val myCustomTransformer = publisher!!.VideoTransformer("myTransformer", logoTransformer)
            videoTransformers.add(backgroundBlur)
            videoTransformers.add(myCustomTransformer)
            publisher!!.setVideoTransformers(videoTransformers)

            audioTransformers.clear()
            val ns = publisher!!.AudioTransformer("NoiseSuppression", "")
            audioTransformers.add(ns)
            publisher!!.setAudioTransformers(audioTransformers)

            isSet = true
            buttonVideoTransformers?.text = "Reset"
        } else {
            videoTransformers.clear()
            publisher!!.setVideoTransformers(videoTransformers)
            audioTransformers.clear()
            publisher!!.setAudioTransformers(audioTransformers)

            isSet = false
            buttonVideoTransformers?.text = "Set"
        }
    }
}
