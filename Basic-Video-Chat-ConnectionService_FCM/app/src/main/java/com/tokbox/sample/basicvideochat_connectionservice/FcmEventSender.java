package com.tokbox.sample.basicvideochat_connectionservice;

import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.telecom.DisconnectCause;
import android.util.Log;

import okhttp3.*;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class FcmEventSender {
    private static final String TAG = VonageManager.class.getSimpleName();
    private static final String SERVER_URL = "https://example.com/notify";  // Replace with your actual server URL
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/<YOUR_PROJECT>/messages:send"; // Replace with your actual FCM project URL

    private static final FcmEventSender instance = new FcmEventSender();

    public static FcmEventSender getInstance() {
        return instance;
    }

    private FcmEventSender() {}

    // IMPORTANT: This network operation should NOT run on the main thread!
    // You need to perform this asynchronously.
    public void notifyCallerOfCallResponse(String callerId, String callerName, boolean accepted) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Send the FCM message to the remote device token(s)
                    // This usually involves making an HTTP POST request to the FCM v1 API
                    // This step is typically done from a secure backend using the Firebase Admin SDK!

                    // In a real app, you'd use Firebase Admin SDK on your backend to send the message
                    // You would NOT send FCM messages directly from the Android client in production this way.
                    String googleCloudToken = getGoogleCloudToken();  // OAuth token

                    // In the server look up the remote user's FCM token(s) based on remoteUserId
                    String remoteDeviceFcmToken = lookupFcmTokenForUserId(callerId);

                    if (!remoteDeviceFcmToken.isEmpty() && !googleCloudToken.isEmpty()) {
                        if(accepted){
                            sendFcmMessage("CALL_ACCEPTED", remoteDeviceFcmToken, callerName, callerId, googleCloudToken);
                        } else {
                            sendFcmMessage("CALL_REJECTED", remoteDeviceFcmToken, callerName, callerId, googleCloudToken);
                        }
                    } else {
                        Log.e(TAG,"FCM token or OAuth Google token not found");
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    // IMPORTANT: This network operation should NOT run on the main thread!
    // You need to perform this asynchronously.
    public void notifyRemoteDeviceOfOutgoingCall(String remoteUserId, String localUserId, String localUserName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Send the FCM message to the remote device token(s)
                    // This usually involves making an HTTP POST request to the FCM v1 API
                    // This step is typically done from a secure backend using the Firebase Admin SDK!

                    // In a real app, you'd use Firebase Admin SDK on your backend to send the message
                    // You would NOT send FCM messages directly from the Android client in production this way.
                    String googleCloudToken = getGoogleCloudToken();  // OAuth token

                    // In the server look up the remote user's FCM token(s) based on remoteUserId
                    String remoteDeviceFcmToken = lookupFcmTokenForUserId(remoteUserId);

                    if (!remoteDeviceFcmToken.isEmpty() && !googleCloudToken.isEmpty()) {
                        sendFcmMessage("INCOMING_CALL", remoteDeviceFcmToken, localUserName, localUserId, googleCloudToken);
                    } else {
                        Log.e(TAG,"FCM token or OAuth Google token not found");
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    // Helper method to get Google Cloud OAuth token
    private String getGoogleCloudToken() {
        // TODO: Replace with your logic to fetch a real OAuth token
        return "";
    }

    // Look up the FCM token for the remote user
    private String lookupFcmTokenForUserId(String userId) {
        // TODO: Replace with logic to query the backend to get the FCM token for a user ID
        return "";
    }

    // Send FCM message to remote device
    private void sendFcmMessage(String messageType, String fcmToken, String localUserName, String localUserId, String googleCloudToken) throws IOException, JSONException {
        JSONObject messageJson;

        switch (messageType) {
            case "INCOMING_CALL":
                messageJson = createIncomingCallFcmMessage(fcmToken, localUserName, localUserId);
                break;

            case "CALL_REJECTED":
            case "CALL_ACCEPTED":
                messageJson = createCallAnsweredFcmMessage(fcmToken, messageType, localUserName, localUserId);
                break;

            default:
                Log.w(TAG, "Unknown message type: " + messageType);
                return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(FCM_API_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + googleCloudToken)
                .post(RequestBody.create(messageJson.toString(), MediaType.parse("application/json")))
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            Log.d("HttpNotifier", "FCM sent successfully from client: " + response.body().string());
        } else {
            Log.e("HttpNotifier", "FCM send failed from client: " + response.code() + " " + response.body().string());
        }
    }

    // Construct the FCM Json message payload
    // You can pass any custom data as you want
    private JSONObject createIncomingCallFcmMessage(String fcmToken, String localUserName, String localUserId) throws JSONException {
        JSONObject messageJson = new JSONObject();
        JSONObject messageObject = new JSONObject();

        messageObject.put("token", fcmToken);

        // Notification payload
        JSONObject notificationObject = new JSONObject();
        notificationObject.put("title", "Incoming Call");
        notificationObject.put("body", localUserName + " is calling...");
        messageObject.put("notification", notificationObject);

        // Data payload
        JSONObject dataObject = new JSONObject();
        dataObject.put("type", "INCOMING_CALL");
        dataObject.put("callerId", localUserId);
        dataObject.put("callerName", localUserName);
        messageObject.put("data", dataObject);

        messageJson.put("message", messageObject);

        return messageJson;
    }

    private JSONObject createCallAnsweredFcmMessage(String fcmToken, String answerType, String localUserName, String localUserId) throws JSONException {
        JSONObject messageJson = new JSONObject();
        JSONObject messageObject = new JSONObject();

        messageObject.put("token", fcmToken);

        // Data payload
        JSONObject dataObject = new JSONObject();
        dataObject.put("type", answerType);
        dataObject.put("callerId", localUserId);
        dataObject.put("callerName", localUserName);
        messageObject.put("data", dataObject);

        messageJson.put("message", messageObject);

        return messageJson;
    }
}
