package com.tokbox.sample.basicvideochat_connectionservice;

import static android.telecom.TelecomManager.PRESENTATION_ALLOWED;

import static androidx.core.content.ContextCompat.getSystemService;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.OutcomeReceiver;
import android.telecom.CallAudioState;
import android.telecom.CallEndpoint;
import android.telecom.CallEndpointException;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.concurrent.Executor;

public class VonageConnection extends Connection implements AudioDeviceSelectionListener{

    private final Context context;
    private String mRoomName;
    private Intent mLaunchIntent;
    private static final int REQUEST_CODE_ROOM_ACTIVITY = 2;
    public static final int ONGOING_CALL_NOTIFICATION_ID = 1;
    private AudioDeviceSelector audioDeviceSelector = AudioDeviceSelector.getInstance();


    public VonageConnection(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void onShowIncomingCallUi() {
        super.onShowIncomingCallUi();
        Log.d("VonageConnection", "onShowIncomingCallUi");

        postIncomingCallNotification(true);

        Intent answeredIntent = new Intent(CallActionReceiver.ACTION_INCOMING_CALL);
        LocalBroadcastManager.getInstance(context).sendBroadcast(answeredIntent);
    }

    @Override
    public void onSilence() {
        super.onSilence();
        postIncomingCallNotification(false);
    }

    public void onPlaceCall() {
        setActive();
        VonageManager.getInstance().initializeSession(API_KEY, SESSION_ID, TOKEN);
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        setActive();
        VonageManager.getInstance().initializeSession(API_KEY, SESSION_ID, TOKEN);
        removeCallNotification();
        updateOngoingCallNotification();

        Intent answeredIntent = new Intent(CallActionReceiver.ACTION_ANSWERED_CALL);
        LocalBroadcastManager.getInstance(context).sendBroadcast(answeredIntent);
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        VonageManager.getInstance().endSession();
        AudioDeviceSelector.getInstance().setAudioDeviceSelectionListener(null);
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        removeCallNotification();

        Intent endedIntent = new Intent(CallActionReceiver.ACTION_CALL_ENDED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(endedIntent);

        destroy();
    }

    @Override
    public void onAbort() {
        super.onAbort();
        onDisconnect();
    }

    @Override
    public void onReject() {
        super.onReject();
        removeCallNotification();

        Intent rejectedIntent = new Intent(CallActionReceiver.ACTION_REJECTED_CALL);
        LocalBroadcastManager.getInstance(context).sendBroadcast(rejectedIntent);

        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));

        destroy();
    }

    @Override
    public void onHold() {
        super.onHold();
        setOnHold();

        VonageManager.getInstance().setMuted(true);
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
        setActive();
        VonageManager.getInstance().setMuted(false);
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);

        if (state == Connection.STATE_ACTIVE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                VonageManager.getInstance().requestAudioFocus(context);
            }
            VonageManager.getInstance().notifyAudioFocusIsActive();
        } else if (state == Connection.STATE_HOLDING) {
            VonageManager.getInstance().notifyAudioFocusIsInactive();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                VonageManager.getInstance().releaseAudioFocus();
            }
        }
        Log.d("VonageConnection", "Connection state changed to: " + state);
    }

    @Override
    public void onAvailableCallEndpointsChanged(@NonNull List<CallEndpoint> endpoints) {
        super.onAvailableCallEndpointsChanged(endpoints);

        if (audioDeviceSelector != null) {
            audioDeviceSelector.onAvailableCallEndpointsChanged(endpoints);
        }
    }

    @Override
    public void onCallEndpointChanged(@NonNull CallEndpoint endpoint) {
        super.onCallEndpointChanged(endpoint);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d("VonageConnection", "Active audio endpoint changed to: " + endpoint.getEndpointType());
        }
        if (audioDeviceSelector != null) {
            audioDeviceSelector.onCallEndpointChanged(endpoint);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void changeCallEndpoint(CallEndpoint endpoint) {
        Executor executor = ContextCompat.getMainExecutor(context);

        requestCallEndpointChange(endpoint, executor, new OutcomeReceiver<Void, CallEndpointException>() {
            @Override
            public void onResult(Void result) {
                Log.d("VonageConnection", "Successfully switched to endpoint: " + endpoint.getEndpointType());
            }

            @Override
            public void onError(CallEndpointException error) {
                Log.e("VonageConnection", "Failed to switch endpoint: " + error.getMessage());
            }
        });
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);

        Log.d("VonageConnection", "Current audio route: " + audioState.getRoute());

        if (audioDeviceSelector != null) {
            audioDeviceSelector.onCallAudioStateChanged(audioState);
        }
    }

    @Override
    public void onAudioDeviceSelected(AudioDeviceSelector.AudioDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (device.getEndpoint() != null) {
                changeCallEndpoint(device.getEndpoint());
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAudioRoute(device.getType());
        }
    }

    @Override
    public void onMuteStateChanged(boolean isMuted) {
        super.onMuteStateChanged(isMuted);

        VonageManager.getInstance().setMuted(isMuted);
    }

    private void postIncomingCallNotification(Boolean isRinging) {
        Notification notification = getIncomingCallNotification(isRinging);
        notification.flags |= Notification.FLAG_INSISTENT;

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(ONGOING_CALL_NOTIFICATION_ID, notification);
    }

    public Notification getIncomingCallNotification(Boolean isRinging) {
        // Create an intent which triggers your fullscreen incoming call user interface.
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification as an ongoing high priority item; this ensures it will show as
        // a heads up notification which slides down over top of the current content.
        final Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, NotificationChannelManager.INCOMING_CALL_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setOnlyAlertOnce(!isRinging);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(true);
        }
        // Set notification content intent to take user to fullscreen UI if user taps on the
        // notification body.
        builder.setContentIntent(pendingIntent);
        // Set full screen intent to trigger display of the fullscreen UI when the notification
        // manager deems it appropriate.
        builder.setFullScreenIntent(pendingIntent, true);

        builder.setSmallIcon(R.drawable.ic_stat_ic_notification);
        builder.setContentTitle("Incoming call");
        builder.setContentText("Mom is calling...");
        builder.setColor(0xFF2196F3);

        Intent answerIntent = new Intent(context, CallActionReceiver.class);
        answerIntent.setAction(CallActionReceiver.ACTION_ANSWER_CALL);
        PendingIntent answerPendingIntent = PendingIntent.getBroadcast(
                context,
                CallActionReceiver.ACTION_ANSWER_CALL_ID,
                answerIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Intent rejectIntent = new Intent(context, CallActionReceiver.class);
        rejectIntent.setAction(CallActionReceiver.ACTION_REJECT_CALL);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                context,
                CallActionReceiver.ACTION_REJECT_CALL_ID,
                rejectIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(new Notification.Action.Builder(
                R.drawable.answer_call, "Answer", answerPendingIntent).build());
        builder.addAction(new Notification.Action.Builder(
                R.drawable.end_call, "Reject", rejectPendingIntent).build());

        return builder.build();
    }

    private void updateOngoingCallNotification() {
        Notification notification = getOngoingCallNotification();
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(ONGOING_CALL_NOTIFICATION_ID, notification);
        }
    }

    public Notification getOngoingCallNotification() {
        Intent hangupIntent = new Intent(context, CallActionReceiver.class);
        hangupIntent.setAction(CallActionReceiver.ACTION_END_CALL);
        PendingIntent hangupPendingIntent = PendingIntent.getBroadcast(
                context,
                CallActionReceiver.ACTION_END_CALL_ID,
                hangupIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, NotificationChannelManager.ONGOING_CALL_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(true);
        }
        builder.setColor(0xFF2196F3);
        builder.setOngoing(true)
                .setContentTitle("Ongoing call")
                .setContentText("Talking with Mom...")
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(
                        R.drawable.end_call, "End call", hangupPendingIntent
                ).build());

        return builder.build();
    }

    private void removeCallNotification() {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.cancel(ONGOING_CALL_NOTIFICATION_ID);
        }
    }
}
