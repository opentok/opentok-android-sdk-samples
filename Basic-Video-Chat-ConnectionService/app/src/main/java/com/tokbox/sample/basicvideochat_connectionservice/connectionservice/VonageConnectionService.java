package com.tokbox.sample.basicvideochat_connectionservice.connectionservice;

import static android.telecom.Connection.CAPABILITY_HOLD;
import static android.telecom.Connection.CAPABILITY_MUTE;
import static android.telecom.Connection.CAPABILITY_SUPPORT_HOLD;
import static android.telecom.Connection.PROPERTY_SELF_MANAGED;
import static android.telecom.TelecomManager.PRESENTATION_ALLOWED;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.tokbox.sample.basicvideochat_connectionservice.VonageManager;
import com.tokbox.sample.basicvideochat_connectionservice.deviceselector.AudioDeviceSelector;
import com.tokbox.sample.basicvideochat_connectionservice.CallActionReceiver;

import java.util.Random;

public class VonageConnectionService extends ConnectionService {
    private static final String TAG = VonageConnectionService.class.getSimpleName();

    private final BroadcastReceiver callEndedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_CALL_ENDED.equals(intent.getAction())) {
                stopForeground(true);
            }
            VonageConnectionHolder.getInstance().setConnection(null);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                callEndedReceiver,
                new IntentFilter(CallActionReceiver.ACTION_CALL_ENDED)
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(callEndedReceiver);
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {

        Uri addressUri = request.getAddress();
        String callerName = addressUri.getQueryParameter("callerName");
        Random random = new Random();
        int randomValue = random.nextInt() * random.nextInt();

        VonageConnection connection = new VonageConnection(getApplicationContext(), callerName, randomValue);
        connection.setInitialized();

        connection.setCallerDisplayName(callerName, PRESENTATION_ALLOWED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connection.setConnectionProperties(PROPERTY_SELF_MANAGED);
        }

        connection.setAudioModeIsVoip(true);
        connection.setVideoState(request.getVideoState());

        int capabilities = CAPABILITY_HOLD | CAPABILITY_SUPPORT_HOLD | CAPABILITY_MUTE;
        connection.setConnectionCapabilities(capabilities);

        AudioDeviceSelector.getInstance().setAudioDeviceSelectionListener(connection);

        VonageConnectionHolder.getInstance().setConnection(connection);

        Notification notification = connection.getOngoingCallNotification();
        startForeground(randomValue, notification);

        connection.onPlaceCall();
        
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Bundle extras = request.getExtras();
        String callerName = extras.getString(PhoneAccountManager.CALLER_NAME);

        Random random = new Random();
        int randomValue = random.nextInt() * random.nextInt();

        VonageConnection connection = new VonageConnection(getApplicationContext(), callerName, randomValue);
        connection.setRinging();
        connection.setCallerDisplayName(callerName, PRESENTATION_ALLOWED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connection.setConnectionProperties(PROPERTY_SELF_MANAGED);
        }

        connection.setAudioModeIsVoip(true);
        connection.setVideoState(request.getVideoState());

        int capabilities = CAPABILITY_HOLD | CAPABILITY_SUPPORT_HOLD | CAPABILITY_MUTE;
        connection.setConnectionCapabilities(capabilities);

        AudioDeviceSelector.getInstance().setAudioDeviceSelectionListener(connection);

        VonageConnectionHolder.getInstance().setConnection(connection);

        Notification notification = connection.getIncomingCallNotification(true);
        startForeground(randomValue, notification);

        return connection;
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Log.e(TAG, "Incoming connection failed: " + request.getAddress());
        VonageConnectionHolder.getInstance().setConnection(null);
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Log.e(TAG, "Outgoing connection failed: " + request.getAddress());
        VonageConnectionHolder.getInstance().setConnection(null);
    }

    @Override
    public void onConnectionServiceFocusGained() {
        super.onConnectionServiceFocusGained();

        VonageManager.getInstance().notifyAudioFocusIsActive();
        Log.d(TAG, "onConnectionServiceFocusGained");
    }

    @Override
    public void onConnectionServiceFocusLost() {
        super.onConnectionServiceFocusLost();

        VonageManager.getInstance().notifyAudioFocusIsInactive();
        Log.d(TAG, "onConnectionServiceFocusLost");
    }
}