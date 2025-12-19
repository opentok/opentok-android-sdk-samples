package com.tokbox.sample.basicvideochatconnectionservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi

class NotificationChannelManager(private val context: Context) {

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun setup() {
        setupIncomingCallChannel()
        setupOngoingCallChannel()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupIncomingCallChannel() {
        val channel = NotificationChannel(
            INCOMING_CALL_CHANNEL_ID, "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        channel.setSound(
            ringtoneUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )

        val mgr = context.getSystemService(
            NotificationManager::class.java
        )
        mgr.createNotificationChannel(channel)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupOngoingCallChannel() {
        val channel = NotificationChannel(
            ONGOING_CALL_CHANNEL_ID, "Ongoing Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val mgr = context.getSystemService(
            NotificationManager::class.java
        )
        mgr.createNotificationChannel(channel)
    }

    companion object {
        const val INCOMING_CALL_CHANNEL_ID: String = "vonage_video_call_channel"
        const val ONGOING_CALL_CHANNEL_ID: String = "vonage_ongoing_video_call_channel"
    }
}
