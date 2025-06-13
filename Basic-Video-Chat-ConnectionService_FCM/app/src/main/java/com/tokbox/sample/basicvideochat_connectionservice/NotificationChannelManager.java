package com.tokbox.sample.basicvideochat_connectionservice;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class NotificationChannelManager {

    public static final String INCOMING_CALL_CHANNEL_ID = "vonage_video_call_channel";
    public static final String ONGOING_CALL_CHANNEL_ID = "vonage_ongoing_video_call_channel";

    private Context context;

    public NotificationChannelManager(Context context) {
        this.context = context;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    void setup() {
        setupIncomingCallChannel();
        setupOngoingCallChannel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupIncomingCallChannel() {
        NotificationChannel channel = new NotificationChannel(INCOMING_CALL_CHANNEL_ID, "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH);

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        channel.setSound(ringtoneUri, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());

        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        mgr.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupOngoingCallChannel() {
        NotificationChannel channel = new NotificationChannel(ONGOING_CALL_CHANNEL_ID, "Incoming Calls",
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        mgr.createNotificationChannel(channel);
    }
}
