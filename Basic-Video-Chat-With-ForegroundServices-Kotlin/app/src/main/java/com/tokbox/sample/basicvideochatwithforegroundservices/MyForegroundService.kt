package com.tokbox.sample.basicvideochatwithforegroundservices

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat


class MyForegroundService : Service() {
    val CHANNEL_ID: String = "MyForegroundService"
    val CHANNEL_NAME: String = "Audio Foreground Service"
    var NOTIFICATION_ID_MIC: Int = 1
    var REQUEST_MIC: Int = 1001

    val TAG: String = MyForegroundService::class.java.simpleName

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Cannot use microphone on background without RECORD_AUDIO permission")
            stopSelf()
        }

        try {
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Audio Call in Progress")
                .setContentText("Your microphone is active while the app is in the background.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID_MIC,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                },
            )

        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                Log.e(TAG, "App not in a valid state to start foreground service")
            } else {
                Log.e(TAG, e.message.toString())
            }
        }
        return START_STICKY
    }
}