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
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/vonageconnectionservice/messages:send";

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
        return "ya29.c.c0ASRK0Ga8BUDaS8zhAW5NhPJMVbJzT_zozgYoipGizCf4AXI5aD8POJZI9w-snzQ7oJuV2Orb9Tv6qxWg1ujIXs6oDIpga6O87XsGQWA6WJ89p8GaKvMO3FNEhJ-l83o-NM3WwsnxOI5q_mnzHPREscEBJx2RjK7OFYaTYuMtJ8n-qF5AEn3HKk9sXSaM75IR2LqB9PZtkX5r_64HLPVkNzA07htFT4aqwflqkB-4hy9MLvhHH7DfmASoJyu0SzcvXuqZsQrkc3TPcALOKDp7gpPaXOGztfY5lGUzsxp04FiR2D_AgsFGq3o8UEya55HHSgQXyqAaxqILXXGGyl_wwgvBKvBeiX9C0uBmIjaJdi7lrHU7yAlIGZPragN387C8prcmfZqxbxqszZbYep4egJ_w7XU7Ryw_QWin8VW-xXSFBe7lfBh0WOc0Zx9ld10SWQYkZsUcJxOtBpUnmcfiVYgS4pYlqI9ldhYmw-W4e4o4p0r2kwFt2f2ZkgduOyUjyQeJ_xte62Xzncw1lM42q_8yY1o5f4FI-qbuSoU4kJcXlIxvwq7k67wrrXni1127f35ltw33nfV2BO936MZtx3e6W05RduY84_x9x39f5SQknoFB2gR2Q7p6cyyMVQbMWRecw0zbJFs4c5Y3egoye0Rp6xQbs9hn35ftemV8kbrju5WFjlZv_3pUrbQVIVdvOiyYyp4-hyBIXYWitavvQgfiVtu_OFnyQVd_acXebyjof7Ue1y63ZcXypztt2uJmx7ekuMfQa3h-Ip1mb_qU1aqf1fYBwtRRUR64RVIbuOwfykf60e_QZp09w6wQxF60UWVWvvQuZ91qRO15XX6y5flimnj0wjdIYkZ62gIqrZxRQ26BW2fdvWoXhdvUVduB06521Y3FJ3ZXjvvF4epbRairlMk5bed220M1hF5tU_euk-k3J9FQFIgivyX691g-rO7-45thdc9XiV322pygtFll4mVvvRVOhMOh5r-ka9F8oIRQgySZn0S";

    }

    // Look up the FCM token for the remote user
    private String lookupFcmTokenForUserId(String userId) {
        // TODO: Replace with logic to query the backend to get the FCM token for a user ID
        return "eOudJ-d0S42XLSTIMAh10Y:APA91bFVit_cVysYE_V3k_atL6Mx5Z9Wj7nXrHwvFqVMcU8yHCNaTR4hRW8PPItFBjeN4B696jAt1GSg-n1YQto-AoP3IBdrAyMCAI6dvpkYp78IJ0-I-aI";  // Hardcoded for testing
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
