package com.tokbox.sample.basicvideochatwithforegroundservices;

import android.Manifest;
import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.lifecycle.LifecycleService;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

public class MyForegroundService extends LifecycleService {
    private static final String CHANNEL_ID = "MyForegroundService";
    private static final String CHANNEL_NAME = "Audio Foreground Service";
    private static final int NOTIFICATION_ID_MIC = 1;
    private static final int REQUEST_MIC = 1001;

    private static final String TAG = MyForegroundService.class.getSimpleName();

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    /**
     * Class used for the client Binder. We know this service always runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MyForegroundService getService() {
            // Return this instance of MyForegroundService so clients can call public methods
            return MyForegroundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot use microphone on background without RECORD_AUDIO permission");
            stopSelf();
        }

        try{

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Audio Call in Progress")
                    .setContentText("Your microphone is active while the app is in the background.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build();

            int type = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            }

            ServiceCompat.startForeground(this, NOTIFICATION_ID_MIC, notification, type);
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e instanceof ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                Log.e(TAG, "App not in a valid state to start foreground service");
            } else {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        }
        return START_STICKY;
    }

}
