package com.tokbox.sample.webviewscreensharing

import android.Manifest
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.opentok.android.*
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session.SessionListener
import com.opentok.android.SubscriberKit.SubscriberListener
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks


class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null
    private lateinit var publisherViewContainer: RelativeLayout
    private lateinit var webViewContainer: WebView
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

            val screenSharingCapturer = ScreenSharingCapturer(this@MainActivity, webViewContainer)

            publisher = Publisher.Builder(this@MainActivity).capturer(screenSharingCapturer).build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen

            webViewContainer.setWebViewClient(WebViewClient())
            val webSettings: WebSettings = webViewContainer.getSettings()
            webSettings.javaScriptEnabled = true
            webViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            webViewContainer.loadUrl("https://www.tokbox.com")
            
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
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: ${stream.streamId} in session: ${session.sessionId}")
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
        publisherViewContainer = findViewById(R.id.publisherview)
        webViewContainer = findViewById(R.id.webview)
        requestPermissions()
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()

        if (isFinishing()) {
            disconnectSession();
        }
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    override fun onDestroy() {
        disconnectSession()
        super.onDestroy()
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
            // Use hardcoded session config
            if (!OpenTokConfig.isValid) {
                finishWithMessage("Invalid OpenTokConfig. ${OpenTokConfig.description}")
                return
            }
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
        session = Session.Builder(this, apiKey, sessionId).build().also {
            it.setSessionListener(sessionListener)
            it.connect(token)
        }
    }

    private fun disconnectSession() {
        if (session == null) {
            return
        }
        if (publisher != null) {
            publisherViewContainer.removeView(publisher!!.view)
            session!!.unpublish(publisher)
            publisher = null
        }
        session!!.disconnect()
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
}