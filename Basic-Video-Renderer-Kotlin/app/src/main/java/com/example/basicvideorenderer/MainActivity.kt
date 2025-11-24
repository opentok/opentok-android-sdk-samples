package com.example.basicvideorenderer
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.basicvideorenderer.ui.theme.BasicVideoRendererTheme
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit

import com.opentok.android.Session;
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit


/**
 * MainActivity - Entry point for the Vonage video renderer example.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // --- Permissions ---
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeSession(
                appId = VonageVideoConfig.APP_ID,
                sessionId = VonageVideoConfig.SESSION_ID,
                token = VonageVideoConfig.TOKEN
            )
        } else {
            Toast.makeText(
                this,
                "Camera and microphone permissions are required to make video calls.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --- OpenTok session and video variables ---
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    // Views observed by Compose
    private var publisherView by mutableStateOf<View?>(null)
    private var subscriberView by mutableStateOf<View?>(null)

    // --- Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VideoChatScreen(
                publisherView = publisherView,
                subscriberView = subscriberView
            )
        }

        requestPermissions()
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    // --- Permissions Helpers ---
    private fun requestPermissions() {
        if (hasPermissions()) {
            initializeSession(
                appId = VonageVideoConfig.APP_ID,
                sessionId = VonageVideoConfig.SESSION_ID,
                token = VonageVideoConfig.TOKEN
            )
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun hasPermissions(): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    // --- Vonage Session Initialization ---
    private fun initializeSession(appId: String, sessionId: String, token: String) {
        Log.i(TAG, "Initializing session with appId=$appId")
        session = Session.Builder(this, appId, sessionId).build().apply {
            setSessionListener(sessionListener)
            connect(token)
        }
    }

    // --- OpenTok Session Listener ---
    private val sessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "Connected to session: ${session.sessionId}")

            publisher = Publisher.Builder(this@MainActivity)
                .renderer(InvertedColorsVideoRenderer(this@MainActivity))
                .build()
                .apply {
                    setPublisherListener(publisherListener)
                    renderer.setStyle(
                        BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL
                    )
                }

            publisherView = publisher?.view
            session.publish(publisher)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "Disconnected from session: ${session.sessionId}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "Stream received: ${stream.streamId}")

            if (subscriber == null) {
                val newSubscriber = Subscriber.Builder(this@MainActivity, stream)
                    .renderer(InvertedColorsVideoRenderer(this@MainActivity))
                    .build()
                    .apply {
                        renderer.setStyle(
                           BaseVideoRenderer.STYLE_VIDEO_SCALE,
                            BaseVideoRenderer.STYLE_VIDEO_FILL
                        )
                        setSubscriberListener(subscriberListener)
                    }

                session.subscribe(newSubscriber)
                subscriber = newSubscriber
                subscriberView = newSubscriber.view
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.i(TAG, "Stream dropped: ${stream.streamId}")
            subscriber?.let {
                subscriberView = null
                subscriber = null
            }
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            Log.e(TAG, "Session error: ${opentokError.message}")
        }
    }

    // --- Publisher Listener ---
    private val publisherListener = object : PublisherKit.PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "Publisher stream created: ${stream.streamId}")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "Publisher stream destroyed: ${stream.streamId}")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            Log.e(TAG, "Publisher error: ${opentokError.message}")
        }
    }

    // --- Subscriber Listener ---
    private val subscriberListener = object : SubscriberKit.SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "Subscriber connected: ${subscriberKit.stream?.streamId}")
        }

        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "Subscriber disconnected: ${subscriberKit.stream?.streamId}")
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            Log.e(TAG, "Subscriber error: ${opentokError.message}")
        }
    }
}