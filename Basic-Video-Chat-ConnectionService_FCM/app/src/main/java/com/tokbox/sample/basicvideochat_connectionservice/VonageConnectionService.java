package com.tokbox.sample.basicvideochat_connectionservice;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

public class VonageConnectionService extends ConnectionService {

    public static final String ACCOUNT_ID = "vonage_video_call";

    private static PhoneAccount phoneAccount;
    private static PhoneAccountHandle handle;
    private static TelecomManager telecomManager;

    public static TelecomManager getTelecomManager() {
        return telecomManager;
    }

    public static void registerPhoneAccount(Context context) {
        telecomManager = (TelecomManager)
                context.getSystemService(Context.TELECOM_SERVICE);

        ComponentName componentName = new ComponentName(context, VonageConnectionService.class);
        handle = new PhoneAccountHandle(componentName, ACCOUNT_ID);

        phoneAccount = PhoneAccount.builder(handle, "Vonage Video")
                //.setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED) // uncomment when using custom UI
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .setHighlightColor(Color.BLUE)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher)) // your app icon
                .build();

        telecomManager.registerPhoneAccount(phoneAccount);
    }

    public static boolean isPhoneAccountEnabled() {
        return phoneAccount.isEnabled();
    }

    public static PhoneAccount getPhoneAccount() {
        return phoneAccount;
    }

    public static PhoneAccountHandle getAccountHandle() {
        return handle;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Bundle extras = request.getExtras();

        // Extract data specified on FCM json
        String roomName = extras.getString("ROOM_NAME");
        String callerId = extras.getString(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS);
        String callerName = extras.getString("CALLER_NAME");

        VonageConnection connection = new VonageConnection(getApplicationContext(), roomName, callerId, callerName);
        connection.setDialing();
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Bundle extras = request.getExtras();

        // Extract your custom extras
        String roomName = extras.getString("ROOM_NAME");
        String callerId = extras.getString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS);
        String callerName = extras.getString("CALLER_NAME");

        VonageConnection connection = new VonageConnection(getApplicationContext(), roomName, callerId, callerName);
        connection.setRinging();
        return connection;
    }
}