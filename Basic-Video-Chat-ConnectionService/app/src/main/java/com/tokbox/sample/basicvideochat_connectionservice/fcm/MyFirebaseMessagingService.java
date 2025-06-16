package com.tokbox.sample.basicvideochat_connectionservice.fcm;

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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.tokbox.sample.basicvideochat_connectionservice.CallActionReceiver;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");

            if (type != null && type.equals("INCOMING_CALL")) {
                handleIncomingCall(data);
            } else {
                Log.w(TAG, "Unknown message type: " + type);
            }
        }
    }

    private void handleIncomingCall(Map<String, String> data) {
        Bundle extras = new Bundle();
        extras.putString("CALLER_ID", data.get("callerId"));
        extras.putString("CALLER_NAME", data.get("callerName"));

        Intent answeredIntent = new Intent(CallActionReceiver.ACTION_NOTIFY_INCOMING_CALL);
        answeredIntent.putExtras(extras);
        LocalBroadcastManager.getInstance(this).sendBroadcast(answeredIntent);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }
}
