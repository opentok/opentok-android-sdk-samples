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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FcmEventSender {
    private static final String TAG = VonageManager.class.getSimpleName();
    private static final String SERVER_URL = "https://example.com/notify";  // Replace with your actual server URL
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/vonageconnectionservice/messages:send"; // Replace with your actual FCM project URL

    private static final FcmEventSender instance = new FcmEventSender();

    public static FcmEventSender getInstance() {
        return instance;
    }
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private FcmEventSender() {}

    // IMPORTANT: This network operation should NOT run on the main thread!
    public void notifyCallerOfCallResponse(String callerId, String callerName, boolean accepted) {
        executor.execute(() -> {
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
        });
    }

    // IMPORTANT: This network operation should NOT run on the main thread!
    public void notifyRemoteDeviceOfOutgoingCall(String remoteUserId, String localUserId, String localUserName) {
        executor.execute(() -> {
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
        });
    }

    // Helper method to get Google Cloud OAuth token
    private String getGoogleCloudToken() {
        // TODO: Replace with your logic to fetch a real OAuth token
        return "ya29.c.c0ASRK0GYzqafX4rwaZSwMdxoFm_0ZQfuKtz3Xm2epEmmHmcPaMeLoFvABSAM4qdId7mOycAURRMTUO2y0iA-yZw0JmiaSdUFHYRofKBnaRPaL-Bhzmc23Slncks7DqqjPvSP8XON3trSpWVnnZ18JBdPL_bTGcM0HkJOVTBV_l0xQ_CgsUFo__egtIOyv8X8PBYZr1SyTdhB5d3D6ttCGio7ZDwZ_u9Ifvnl3DObrPoH1EvbjSuvpf80YMKbj-KX1BK5o36AhqjgkgROQoRQ3XAVYPinxq8Ke_l6nCGcfgTGf_i5sAaPoGjK0Ix1k_KglQ6aB7ikLOlG62dDMR4lgKZHWHh5ZcEi1EFHDO_xZInJg0gpj-G6sqphp2QE387Dd3QWm2dtVrSuSk_0Y09IVnRszx2u83vsW-kFR91kptchXgmvJamocmnd3nXZf3iOJFg7n5xZcuz7mVo45-eOXkt_FjkvXkOzoBWygSdJQ4dbjR6Ysf70uv5OWz8jewt_pfbmf8so5ee4hII2oYiJjSjY-vmzzXdxz8_VF_yp-QR-SyIOVI9Y0vfZfivn_mZSIIwvw6RJdqYl4grv6UOh1WWB6-cu5ImjBWh1w8Ih0MspMUoca_dFjbRbz-fMprgJF-ZpaXU6Ifh1Q9OmYmlpyFdz8R5Vsfkn4et88vke27-jYXf9BcfM003n3yUkcmQSYWpM5fc0yjO9csqM78wmqRSFXBMpM9SQca7QRJtFbf4a8j4WVspY4yvu7SBYJ8OI1h1iUfapZQWQn757SSY4kO30hRFW2lmRfrBkn_3nl0rkd7IiqF668JbVVvzXO5y-x1JtVpQ7ntcn3lFJjRnynhOgux3nBkFVJ5h75lJzdmIq0w1cRrogI5uj1I6SxgyrccgQmUsZuQX1ZecbOF46vMrRjXhpgYeS3B_iclRVnyy201m3-da_-R6cg6y9I7l3pSad83Wy71zt6xbscZX0M54v44IzSbwiRRaeoBnv87Wk4enlr5yIj-9F";

    }

    // Look up the FCM token for the remote user
    private String lookupFcmTokenForUserId(String userId) {
        // TODO: Replace with logic to query the backend to get the FCM token for a user ID
        return "fThVtiM1QNWgOr_20f5BME:APA91bFionYfi2KNNllq7vtuwsLCfeH_JNd9z0KKDomN7wVUdlmdR-MZzOjiBV6_Q5HoG02D2Qq7S_AZWbkLH0jJQuWqybLVMIERPA2WARqOTQPNeQTdsk8";
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
