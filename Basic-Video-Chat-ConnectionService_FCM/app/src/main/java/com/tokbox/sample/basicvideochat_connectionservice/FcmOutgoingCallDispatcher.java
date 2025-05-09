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

public class FcmOutgoingCallDispatcher {

    private static final String SERVER_URL = "https://example.com/notify";  // Replace with your actual server URL
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/vonageconnectionservice/messages:send";

    // IMPORTANT: This network operation should NOT run on the main thread!
    // You need to perform this asynchronously.
    public void notifyRemoteDeviceOfOutgoingCall(VonageConnection conn, String remoteUserId, String roomName,
                                                 String localUserId, String localUserName) {
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
                        sendFcmMessage(remoteDeviceFcmToken, localUserName, localUserId, googleCloudToken);
                    } else {
                        handleFailure(conn, "FCM token or OAuth Google token not found");
                    }
                } catch (Exception e) {
                    handleFailure(conn, "Error sending FCM notification: " + e.getMessage());
                }
            }
        }).start();
    }

    // Helper method to get Google Cloud OAuth token
    private String getGoogleCloudToken() {
        // TODO: Replace with your logic to fetch a real OAuth token
        return "ya29.c.c0ASRK0GZFVYqUyKYP6-8a7onhafqtG1teULOteiAMrDK3byei9MSFMuUnYlnX0_Pxo7mKYxsNxmmyBSeS2SUUM8rgf5oRZw32X_ni4XJT2q5ONcy-XBceDCMC-VBCkIwogDhk_ISqVgyEYCNGGlsgczbefn3PeWf7N39qJXOOEgi3HpO4oYWW3AxLiFxli4muY4NLyvrRhc915cLgi-6XfiaEzvcEqGfKzuhekqcf2oi5owMkwdWi8PBC-EKIhvOKZqcno3q0MjVdoq7VUsool7zvj9OSeLfeRS_LvM3Z2iAKF1WAC189Gw7XJX60gty3Q8C3GXZBJbLFjg5rmdB_qU3V8OVidXQTECRh_TjeQbDsjhOUH_ca3mgmE385CO_XgbZh6-Q61Qhxx2uVmm5gblIkazwrlbRik50q-VWiQr4r-QZOUXsacJWgrg0r2cdxFpFQeg4BS5vkOIeJl6eS5UVvtsw-kaihwYvx1cu9pJkBMjUMO-55dVm4OuYQQkQ7B2skpBqgXB7IsqxqgOnzro-BOgvpshxd2fVxlpIwV1QQ_6yjrsFb-Ibq8BoIUr-pbUjuMjkJUy9x2g8-xbVsQmJip8f_OMwxffyRn9mOtlv3keOefl9RcdRUq2-nfI31_4YVW8XRoyWwrgB-4tstm1gzJ2hkcWtBjW_krtV5V0oZbQtVB_cxg5BopdoFSeq5mJgwbjm8gvyuXcltW0jl0gbaQ5zeaSXiiI9dbQ_-Z-i-1waUV07SnqMhcaSQ_izJ8zz9touYSR72Vu3FjV8tyap4c7ycqrBkpzIY6QZxrZ-x0vz6522pS_6JZB93fzWfU5lzQRVn3ggzjukhn3JUjsRU-1zX7oRv9wkM_p_3fwFxyYgcwuRWIBlMeQUg7xpaxiZmIF9ydaQw22v-9-yFF196nvhi6vt9teS7_Qygvw1fccuFSvi7-79tcVJRBtIlbMn70ggO28jSuziswtockSJuc75f-U1w99F_8FYzr7cyQujk5VJtwb5";  // Temporary token for testing
    }

    // Look up the FCM token for the remote user
    private String lookupFcmTokenForUserId(String userId) {
        // TODO: Replace with logic to query the backend to get the FCM token for a user ID
        return "d4BASvIWTHiO5gA9hEcuIt:APA91bEZS2gzA-0V5mq2MJv2ZPAK7tOC6FQcc3L5J0FRcQ7XZoWlhF-vHPanLzgem4TDw4IxBKUBTsmkzVpLM2yKAkL9pM-mwH5ox-vFulKxvBHKNIcQ97E";  // Hardcoded for testing
    }

    // Send FCM message to remote device
    private void sendFcmMessage(String fcmToken, String localUserName, String localUserId, String googleCloudToken) throws IOException, JSONException {
        JSONObject messageJson = createFcmMessage(fcmToken, localUserName, localUserId);

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
    private JSONObject createFcmMessage(String fcmToken, String localUserName, String localUserId) throws JSONException {
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
        dataObject.put("apiKey", API_KEY);
        dataObject.put("sessionId", SESSION_ID);
        dataObject.put("token", TOKEN);
        messageObject.put("data", dataObject);

        messageJson.put("message", messageObject);

        return messageJson;
    }

    // Handle failure (e.g., when the user is not found or network fails)
    private void handleFailure(VonageConnection conn, String errorMessage) {
        Log.e("HttpNotifier", errorMessage);
        // Handle failure, signal disconnect to Telecom
        conn.setDisconnected(new DisconnectCause(DisconnectCause.ERROR, errorMessage));
        conn.destroy();
    }
}
