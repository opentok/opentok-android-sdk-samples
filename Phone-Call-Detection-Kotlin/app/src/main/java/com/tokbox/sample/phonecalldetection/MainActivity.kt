package com.tokbox.sample.phonecalldetection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.opentok.android.*
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session.SessionListener
import com.opentok.android.SubscriberKit.SubscriberListener
import com.tokbox.sample.phonecalldetection.MainActivity
import com.tokbox.sample.phonecalldetection.OpenTokConfig.description
import com.tokbox.sample.phonecalldetection.OpenTokConfig.isValid
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks


class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null
    private var context: Context? = null

    private lateinit var publisherViewContainer: FrameLayout
    private lateinit var subscriberViewContainer: FrameLayout

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

            startVideoPublish(session)
            registerPhoneListener()
        }

        private fun startVideoPublish(session: Session) {
            publisher = Publisher.Builder(this@MainActivity).build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.renderer?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            publisherViewContainer.addView(publisher?.view)

            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }

            session.publish(publisher)
        }

        private fun hasPhoneStatePermission(): Boolean {
            if (Build.VERSION.SDK_INT >= 31) {
                if (context!!.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Some features may not be available unless the phone permissions has been granted explicitly " +
                            "in the App settings.")
                    return false
                }
            }
            return true
        }

        private fun registerPhoneListener() {
            var telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

            if (!hasPhoneStatePermission()) {
                Log.d(TAG, "No Phone State permissions. Register phoneStateListener cannot " +
                        "be completed.");
                return;
            }

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: ${session.sessionId}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received ${stream.streamId} in session: ${session.sessionId}")

            if (subscriber == null) {
                subscriber = Subscriber.Builder(this@MainActivity, stream).build()

                subscriber?.renderer?.setStyle(
                    BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL
                )

                subscriber?.setSubscriberListener(subscriberListener)
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
    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            super.onCallStateChanged(state, incomingNumber)

            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    publisher?.publishVideo = true
                    publisher?.publishAudio = true
                }
                TelephonyManager.CALL_STATE_RINGING -> Log.d("onCallStateChanged", "CALL_STATE_RINGING")
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d("onCallStateChanged", "CALL_STATE_OFFHOOK")
                    publisher?.publishVideo = false
                    publisher?.publishAudio = false
                }
                else -> Log.d("onCallStateChanged", "Unknown Phone State !")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        publisherViewContainer = findViewById(R.id.publisher_container)
        subscriberViewContainer = findViewById(R.id.subscriber_container)
        requestPermissions()
    }

    override fun onPause() {
        super.onPause()

        if (session != null) {
            session?.onPause()
        }
    }

    override fun onResume() {
        super.onResume()

        if (session != null) {
            session?.onResume()
        }
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
            if (!isValid) {
                finishWithMessage("Invalid OpenTokConfig. $description")
                return
            }

            setContext(this);
            
            initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN)
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_video_app),
                PERMISSIONS_REQUEST_CODE,
                *perms
            )
        }
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
        session = Session.Builder(this, apiKey, sessionId).build()
        session?.setSessionListener(sessionListener)
        session?.connect(token)
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

    private fun setContext(_context: Context) {
        context = _context
    }
}