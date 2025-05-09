package com.tokbox.sample.basicvideochat_connectionservice;

import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VonageConnectionService extends ConnectionService {
    private static final String TAG = VonageConnectionService.class.getSimpleName();

    private static CallEventListener listener;

    public static void setCallEventListener(CallEventListener l) {
        listener = l;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Uri destinationUri = request.getAddress();

        String userIdToCall = destinationUri.getSchemeSpecificPart();
        String roomName = destinationUri.getQueryParameter("roomName");
        String callerId = destinationUri.getQueryParameter("callerId");
        String callerName = destinationUri.getQueryParameter("callerName");

        VonageConnection connection = new VonageConnection(getApplicationContext(), API_KEY, SESSION_ID, TOKEN, callerId, callerName);
        connection.setDialing();

        // Notify *remote* device via FCM
        FcmOutgoingCallDispatcher outgoingCallDispatcher = new FcmOutgoingCallDispatcher();
        outgoingCallDispatcher.notifyRemoteDeviceOfOutgoingCall(connection, userIdToCall, roomName, callerId, callerName);

        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Bundle extras = request.getExtras();

        // Extract your custom extras
        String apiKey = extras.getString("API_KEY");
        String sessionId = extras.getString("SESSION_ID");
        String token = extras.getString("TOKEN");
        String callerId = extras.getString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS);
        String callerName = extras.getString("CALLER_NAME");

        VonageConnection connection = new VonageConnection(getApplicationContext(), apiKey, sessionId, token, callerId, callerName);
        connection.setRinging();

        if (listener != null) {
            listener.onIncomingCall(callerName, "Incoming Call");
        }

        return connection;
    }
}