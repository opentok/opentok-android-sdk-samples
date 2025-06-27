package com.vonage.basic_video_chat_connectionservice.connectionservice

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.vonage.basic_video_chat_connectionservice.Call
import com.vonage.basic_video_chat_connectionservice.CallActionReceiver
import com.vonage.basic_video_chat_connectionservice.CallHolder
import com.vonage.basic_video_chat_connectionservice.CallState
import com.vonage.basic_video_chat_connectionservice.VonageManager
import com.vonage.basic_video_chat_connectionservice.deviceselector.AudioDeviceSelector
import dagger.hilt.android.AndroidEntryPoint
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class VonageConnectionService : ConnectionService() {

    @Inject
    lateinit var vonageManager: VonageManager
    @Inject
    lateinit var audioDeviceSelector: AudioDeviceSelector
    @Inject
    lateinit var callHolder: CallHolder

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val addressUri = request.address
        val callerName = addressUri.getQueryParameter("callerName") ?: ""
        val random = Random()
        val randomValue = random.nextInt() * random.nextInt()

        val call = Call(
            callID = randomValue,
            name = callerName,
            state = CallState.CONNECTING
        )
        callHolder.setCall(call)

        val connection = VonageConnection(
            context = applicationContext,
            audioDeviceSelector = audioDeviceSelector,
            vonageManager = vonageManager,
            remoteName = callerName,
            callNotificationId = randomValue,
            callHolder = callHolder
        )

        connection.onCallEnded = {
            connection.onCallEnded = null
            onCallEnded()
        }

        connection.setInitialized()

        connection.setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        }

        connection.audioModeIsVoip = true
        connection.videoState = request.videoState

        val capabilities =
            Connection.CAPABILITY_HOLD or Connection.CAPABILITY_SUPPORT_HOLD or Connection.CAPABILITY_MUTE
        connection.connectionCapabilities = capabilities

        audioDeviceSelector.listener = connection

        VonageConnectionHolder.connection = connection

        val notification: Notification = connection.ongoingCallNotification
        startForeground(randomValue, notification)

        connection.onPlaceCall()

        return connection
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val extras = request.extras
        val callerName = extras.getString(PhoneAccountManager.CALLER_NAME) ?: ""

        val random = Random()
        val randomValue = random.nextInt() * random.nextInt()

        val call = Call(
            callID = randomValue,
            name = callerName,
            state = CallState.CONNECTING
        )
        callHolder.setCall(call)

        val connection = VonageConnection(
            context = applicationContext,
            audioDeviceSelector = audioDeviceSelector,
            vonageManager = vonageManager,
            remoteName = callerName,
            callNotificationId = randomValue,
            callHolder = callHolder
        )
        connection.onCallEnded = {
            connection.onCallEnded = null
            onCallEnded()
        }
        connection.setRinging()
        connection.setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        }

        connection.audioModeIsVoip = true
        connection.videoState = request.videoState

        val capabilities =
            Connection.CAPABILITY_HOLD or Connection.CAPABILITY_SUPPORT_HOLD or Connection.CAPABILITY_MUTE
        connection.connectionCapabilities = capabilities

        audioDeviceSelector.listener = connection

        VonageConnectionHolder.connection = connection

        val notification = connection.getIncomingCallNotification(true)
        startForeground(randomValue, notification)

        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Incoming connection failed: " + request.address)
        VonageConnectionHolder.connection = null
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Outgoing connection failed: " + request.address)
        VonageConnectionHolder.connection = null
    }

    override fun onConnectionServiceFocusGained() {
        super.onConnectionServiceFocusGained()

        vonageManager.notifyAudioFocusIsActive()
        Log.d(TAG, "onConnectionServiceFocusGained")
    }

    override fun onConnectionServiceFocusLost() {
        super.onConnectionServiceFocusLost()

        vonageManager.notifyAudioFocusIsInactive()
        Log.d(TAG, "onConnectionServiceFocusLost")
    }

    private fun onCallEnded() {
        stopForeground(true)
        VonageConnectionHolder.connection = null
        callHolder.setCall(null)
    }

    companion object {
        private val TAG: String = VonageConnectionService::class.java.simpleName
    }
}