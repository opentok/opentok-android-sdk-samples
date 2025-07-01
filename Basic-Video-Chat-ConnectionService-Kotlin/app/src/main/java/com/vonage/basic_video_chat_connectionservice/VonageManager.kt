package com.vonage.basic_video_chat_connectionservice

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.telecom.DisconnectCause
import com.opentok.android.AudioDeviceManager
import com.opentok.android.BaseAudioDevice.AudioFocusManager
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import com.opentok.android.SubscriberKit.SubscriberListener
import com.vonage.basic_video_chat_connectionservice.connectionservice.VonageConnectionHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class VonageManager(
    private val context: Context,
    private val audioDeviceManager: AudioDeviceManager,
    private val callHolder: CallHolder
) {
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null
    private var audioFocusManager: AudioFocusManager? = null

    private val _errorFlow = MutableStateFlow<OpentokError?>(null)
    val errorFlow: StateFlow<OpentokError?> = _errorFlow.asStateFlow()

    private val publisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.streamId)
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(
                TAG,
                "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.streamId
            )
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            handleError(opentokError)
        }
    }

    private val sessionListener: Session.SessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "onConnected: Connected to session: " + session.sessionId)

            if (publisher != null) {
                publisher!!.destroy()
            }

            publisher = Publisher.Builder(context).build()
            publisher!!.setPublisherListener(publisherListener)
            publisher!!.renderer
                .setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)

            callHolder.setPublisherView(publisher!!.view)

            if (publisher!!.view is GLSurfaceView) {
                (publisher!!.view as GLSurfaceView).setZOrderOnTop(true)
            }

            session.publish(publisher)

            CoroutineScope(Dispatchers.IO).launch {
                val currentState = callHolder.callStateFlow.firstOrNull()
                if (currentState == CallState.ANSWERING || currentState == CallState.DIALING) {
                    callHolder.updateCallState(CallState.CONNECTED)
                }
            }
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: " + session.sessionId)

            callHolder.setPublisherView(null)
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(
                TAG,
                "onStreamReceived: New Stream Received " + stream.streamId + " in session: " + session.sessionId
            )

            if (subscriber == null) {
                subscriber = Subscriber.Builder(context, stream).build()
                subscriber!!.renderer.setStyle(
                    BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL
                )
                subscriber!!.setSubscriberListener(subscriberListener)
                session.subscribe(subscriber)
                callHolder.addSubscriberView(streamId = stream.streamId, view = subscriber!!.view)
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(
                TAG,
                "onStreamDropped: Stream Dropped: " + stream.streamId + " in session: " + session.sessionId
            )

            callHolder.removeSubscriberView(streamId = stream.streamId)
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            handleError(opentokError)
        }
    }

    var subscriberListener: SubscriberListener = object : SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.d(
                TAG,
                "onConnected: Subscriber connected. Stream: " + subscriberKit.stream.streamId
            )
        }

        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.d(
                TAG,
                "onDisconnected: Subscriber disconnected. Stream: " + subscriberKit.stream.streamId
            )
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            Log.e(
                TAG,
                "onError: Subscriber did error ${opentokError.message}. Stream: " + subscriberKit.stream.streamId
            )
        }
    }

    fun initializeSession(apiKey: String, sessionId: String, token: String) {
        Log.i(TAG, "apiKey: $apiKey")
        Log.i(TAG, "sessionId: $sessionId")
        Log.i(TAG, "token: $token")

        session = Session.Builder(context.applicationContext, apiKey, sessionId).build()
        session!!.setSessionListener(sessionListener)
        session!!.connect(token)
    }

    fun onResume() {
        if (session != null) session!!.onResume()
    }

    fun onPause() {
        if (session != null) session!!.onPause()
    }

    fun endSession() {
        callHolder.clear()

        if (subscriber != null) {
            if (session != null) {
                session!!.unsubscribe(subscriber)
            }
        }

        if (publisher != null) {
            if (session != null) {
                session!!.unpublish(publisher)
            }
            publisher!!.destroy()
        }


        if (session != null) {
            session!!.disconnect()
        }

        session = null
        publisher = null
        subscriber = null
    }

    fun setAudioFocusManager() {
        audioFocusManager = audioDeviceManager.audioFocusManager

        if (audioFocusManager == null) {
            throw RuntimeException("Audio Focus Manager should have been granted")
        } else {
            audioFocusManager!!.setRequestAudioFocus(false)
        }
    }

    fun notifyAudioFocusIsActive() {
        Log.d("VonageCallManager", "notifyAudioFocusIsActive() called")
        if (audioFocusManager == null) {
            throw RuntimeException("Audio Focus Manager should have been granted")
        }
        audioFocusManager!!.audioFocusActivated()
    }

    fun notifyAudioFocusIsInactive() {
        Log.d("VonageCallManager", "notifyAudioFocusIsInactive() called")
        if (audioFocusManager == null) {
            throw RuntimeException("Audio Focus Manager should have been granted")
        }
        audioFocusManager!!.audioFocusDeactivated()
    }

    fun endCall() {
        VonageConnectionHolder.connection?.onDisconnect()
    }

    fun setMuted(isMuted: Boolean) {
        if (publisher != null) {
            publisher!!.publishAudio = !isMuted
        }
    }

    fun handleError(error: OpentokError, terminateCall: Boolean = true) {
        _errorFlow.value = error

        if (terminateCall) {
            Handler(Looper.getMainLooper()).postDelayed({
                VonageConnectionHolder.connection?.onDisconnect(cause = DisconnectCause.ERROR)
                endSession()
            }, 500)
        }
    }

    fun clearError() {
        _errorFlow.value = null
    }

    companion object {
        private val TAG: String = VonageManager::class.java.simpleName
    }
}