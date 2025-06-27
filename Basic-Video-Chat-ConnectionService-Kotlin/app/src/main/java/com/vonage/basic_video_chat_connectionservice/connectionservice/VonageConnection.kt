package com.vonage.basic_video_chat_connectionservice.connectionservice

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.vonage.basic_video_chat_connectionservice.CallActionReceiver
import com.vonage.basic_video_chat_connectionservice.CallHolder
import com.vonage.basic_video_chat_connectionservice.CallState
import com.vonage.basic_video_chat_connectionservice.MainActivity
import com.vonage.basic_video_chat_connectionservice.NotificationChannelManager
import com.vonage.basic_video_chat_connectionservice.OpenTokConfig.API_KEY
import com.vonage.basic_video_chat_connectionservice.OpenTokConfig.SESSION_ID
import com.vonage.basic_video_chat_connectionservice.OpenTokConfig.TOKEN
import com.vonage.basic_video_chat_connectionservice.R
import com.vonage.basic_video_chat_connectionservice.VonageManager
import com.vonage.basic_video_chat_connectionservice.deviceselector.AudioDeviceSelectionListener
import com.vonage.basic_video_chat_connectionservice.deviceselector.AudioDeviceSelector

class VonageConnection(
    private val context: Context,
    private val audioDeviceSelector: AudioDeviceSelector,
    private val vonageManager: VonageManager,
    private val callHolder: CallHolder,
    private val remoteName: String,
    private val callNotificationId: Int
): Connection(), AudioDeviceSelectionListener {

    var onCallEnded: (()->Unit)? = null

    override fun onSilence() {
        super.onSilence()
        Log.d(TAG, "onSilence")

        postIncomingCallNotification(false)
    }

    fun onPlaceCall() {
        Log.d(TAG, "onPlaceCall")
        setActive()
        vonageManager.initializeSession(API_KEY, SESSION_ID, TOKEN)

        callHolder.updateCallState(CallState.DIALING)
    }

    override fun onAnswer() {
        super.onAnswer()
        Log.d(TAG, "onAnswer")

        setActive()
        vonageManager.initializeSession(API_KEY, SESSION_ID, TOKEN)
        postIncomingCallNotification(false)
        updateOngoingCallNotification()

        callHolder.updateCallState(CallState.ANSWERING)
    }

    override fun onDisconnect() {
        super.onDisconnect()
        Log.d(TAG, "onDisconnect")

        callHolder.updateCallState(CallState.DISCONNECTED)

        vonageManager.endSession()
        audioDeviceSelector.listener = null
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))

        destroy()

        onCallEnded?.invoke()
    }

    override fun onAbort() {
        super.onAbort()
        Log.d(TAG, "onAbort")

        onDisconnect()
    }

    override fun onReject() {
        super.onReject()
        Log.d(TAG, "onReject")

        callHolder.updateCallState(CallState.DISCONNECTED)

        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))

        destroy()

        onCallEnded?.invoke()
    }

    override fun onHold() {
        super.onHold()
        Log.d(TAG, "onHold")

        setOnHold()
        vonageManager.setMuted(true)

        callHolder.updateCallState(CallState.HOLDING)
    }

    override fun onUnhold() {
        super.onUnhold()
        Log.d(TAG, "onUnhold")

        setActive()
        vonageManager.setMuted(false)

        callHolder.updateCallState(CallState.CONNECTED)
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
        Log.d(TAG, "onStateChanged " + stateToString(state))
    }

    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(endpoints)
        Log.d(TAG, "onAvailableCallEndpointsChanged")

        audioDeviceSelector.onAvailableCallEndpointsChanged(endpoints)
    }

    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
        super.onCallEndpointChanged(endpoint)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d("VonageConnection", "Active audio endpoint changed to: " + endpoint.endpointType)
        }

        audioDeviceSelector.onCallEndpointChanged(endpoint)
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun changeCallEndpoint(endpoint: CallEndpoint) {
        val executor = ContextCompat.getMainExecutor(context)

        requestCallEndpointChange(
            endpoint,
            executor,
            object : OutcomeReceiver<Void?, CallEndpointException> {
                override fun onResult(result: Void?) {
                    Log.d(
                        "VonageConnection",
                        "Successfully switched to endpoint: " + endpoint.endpointType
                    )
                }

                override fun onError(error: CallEndpointException) {
                    Log.e("VonageConnection", "Failed to switch endpoint: " + error.message)
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)

        Log.d("VonageConnection", "Current audio route: " + state?.route)

        state?.let {
            audioDeviceSelector.onCallAudioStateChanged(it)
        }
    }


    override fun onAudioDeviceSelected(device: AudioDeviceSelector.AudioDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            device.endpoint?.let {
                changeCallEndpoint(it)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAudioRoute(device.type)
        }
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        Log.d(TAG, "onMuteStateChanged")

        vonageManager.setMuted(isMuted)
    }

    private fun postIncomingCallNotification(isRinging: Boolean) {
        val notification = getIncomingCallNotification(isRinging)
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        )
        notificationManager.notify(callNotificationId, notification)
    }

    fun getIncomingCallNotification(isRinging: Boolean): Notification {
        // Create an intent which triggers your fullscreen incoming call user interface.
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.flags = Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClass(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        // Build the notification as an ongoing high priority item; this ensures it will show as
        // a heads up notification which slides down over top of the current content.
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, NotificationChannelManager.INCOMING_CALL_CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
        builder.setOngoing(true)
        builder.setPriority(Notification.PRIORITY_HIGH)
        builder.setOnlyAlertOnce(!isRinging)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(true)
        }
        // Set notification content intent to take user to fullscreen UI if user taps on the
        // notification body.
        builder.setContentIntent(pendingIntent)
        // Set full screen intent to trigger display of the fullscreen UI when the notification
        // manager deems it appropriate.
        builder.setFullScreenIntent(pendingIntent, true)

        builder.setSmallIcon(R.drawable.ic_stat_ic_notification)
        builder.setContentTitle("Incoming call")
        builder.setContentText("$remoteName is calling...")
        builder.setColor(-0xde690d)

        val answerIntent = Intent(
            context,
            CallActionReceiver::class.java
        )
        answerIntent.action = CallActionReceiver.ACTION_ANSWER_CALL
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            CallActionReceiver.ACTION_ANSWER_CALL_ID,
            answerIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(
            context,
            CallActionReceiver::class.java
        )
        rejectIntent.action = CallActionReceiver.ACTION_REJECT_CALL
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            CallActionReceiver.ACTION_REJECT_CALL_ID,
            rejectIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val caller = Person.Builder()
                .setName(remoteName)
                .setImportant(true)
                .build()
            builder.style = Notification.CallStyle.forIncomingCall(
                caller,
                rejectPendingIntent,
                answerPendingIntent
            )
        } else {
            builder.addAction(
                Notification.Action.Builder(
                    R.drawable.answer_call, "Answer", answerPendingIntent
                ).build()
            )
            builder.addAction(
                Notification.Action.Builder(
                    R.drawable.end_call, "Reject", rejectPendingIntent
                ).build()
            )
        }

        return builder.build()
    }

    private fun updateOngoingCallNotification() {
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        )
        if (notificationManager != null) {
            val notification = ongoingCallNotification
            notificationManager.notify(callNotificationId, notification)
        }
    }

    val ongoingCallNotification: Notification
        get() {
            val hangupIntent = Intent(
                context,
                CallActionReceiver::class.java
            )
            hangupIntent.action = CallActionReceiver.ACTION_END_CALL
            val hangupPendingIntent = PendingIntent.getBroadcast(
                context,
                CallActionReceiver.ACTION_END_CALL_ID,
                hangupIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, NotificationChannelManager.ONGOING_CALL_CHANNEL_ID)
            } else {
                Notification.Builder(context)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setColorized(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val caller = Person.Builder()
                    .setName(remoteName)
                    .setImportant(true)
                    .build()
                builder.style = Notification.CallStyle.forOngoingCall(caller, hangupPendingIntent)
            } else {
                builder.addAction(
                    Notification.Action.Builder(
                        R.drawable.end_call, "End call", hangupPendingIntent
                    ).build()
                )
            }

            builder.setColor(-0xde690d)
            builder.setOngoing(true)
                .setContentTitle("Ongoing call")
                .setContentText("Talking with $remoteName...")
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setOnlyAlertOnce(true)
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis())

            return builder.build()
        }

    companion object {
        private val TAG: String = VonageConnection::class.java.simpleName
    }
}
