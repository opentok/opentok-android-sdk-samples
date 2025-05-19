package com.tokbox.sample.basicvideochat_connectionservice;

import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.net.Uri;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

public class VonageConnectionService extends ConnectionService {
    private static final String TAG = VonageConnectionService.class.getSimpleName();

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Uri destinationUri = request.getAddress();

        String userIdToCall = destinationUri.getSchemeSpecificPart();
        String callerId = destinationUri.getQueryParameter("callerId");
        String callerName = destinationUri.getQueryParameter("callerName");

        // To showcase functionality we always join same session
        VonageConnection connection = new VonageConnection(getApplicationContext(), API_KEY, SESSION_ID, TOKEN, callerId, callerName);
        VonageManager.getInstance().setCurrentConnection(connection);
        connection.setDialing();

        // Notify *remote* device via FCM of call
        FcmEventSender.getInstance().notifyRemoteDeviceOfOutgoingCall(userIdToCall, callerId, callerName);

        // Start Vonage session
        connection.onPlaceCall();

        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Bundle extras = request.getExtras();

        String callerId = extras.getString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS);
        String callerName = extras.getString("CALLER_NAME");

        VonageConnection connection = new VonageConnection(getApplicationContext(), API_KEY, SESSION_ID, TOKEN, callerId, callerName);
        VonageManager.getInstance().setCurrentConnection(connection);
        connection.setRinging();

        return connection;
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Log.e(TAG, "Incoming connection failed: " + request.getAddress());
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount,
                                                 ConnectionRequest request) {
        Log.e(TAG, "Outgoing connection failed: " + request.getAddress());
    }

}