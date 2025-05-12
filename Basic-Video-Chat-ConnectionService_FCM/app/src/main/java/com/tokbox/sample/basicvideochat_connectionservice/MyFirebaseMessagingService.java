package com.tokbox.sample.basicvideochat_connectionservice;

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    private static CallEventListener listener;

    public static void setCallEventListener(CallEventListener l) {
        listener = l;
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");

            /*
            type            | What it does?
            INCOMING_CALL   | Shows system call UI
            CALL_REJECTED   | Ends incoming UI (caller hung up)
            CALL_ACCEPTED   | Signals call picked up
             */

            switch (type) {
                case "INCOMING_CALL":
                    handleIncomingCall(data);
                    break;

                case "CALL_REJECTED":
                    handleCallCanceled(data);
                    break;

                case "CALL_ACCEPTED":
                    handleCallAnswered(data);
                    break;

                default:
                    Log.w(TAG, "Unknown message type: " + type);
                    break;
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String notificationBody = remoteMessage.getNotification().getBody();
            if (remoteMessage.getNotification().getBody() != null) {
                NotificationHelper.sendNotification(getApplicationContext(), notificationBody);
            }
        }
    }

    private void handleIncomingCall(Map<String, String> data) {
        
        PhoneAccountHandle handle = PhoneAccountManager.getAccountHandle();

        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        extras.putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, data.get("callerId"));
        extras.putBoolean(TelecomManager.METADATA_IN_CALL_SERVICE_UI, true);
        extras.putString("CALLER_NAME", data.get("callerName"));

        TelecomManager telecomManager = PhoneAccountManager.getTelecomManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(telecomManager.isIncomingCallPermitted(handle)){
                telecomManager.addNewIncomingCall(handle, extras);
                if (listener != null) {
                    listener.onIncomingCall(data.get("callerName"), "Incoming Call");
                }
            }
        }
    }

    private void handleCallCanceled(Map<String, String> data) {
        if (listener != null) {
            listener.onIncomingCall(data.get("callerName"), "Call Cancelled");
        }
        Log.d(TAG, "Call canceled by: " + data.get("callerId"));
    }

    private void handleCallAnswered(Map<String, String> data) {
        if (listener != null) {
            listener.onIncomingCall(data.get("callerName"), "");
        }
        Log.d(TAG, "Call answered by: " + data.get("callerId"));
    }

    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }
}
