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
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED) // uncomment when using custom UI
                //.setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .setSupportedUriSchemes(Collections.singletonList("vonagecall"))
                .setHighlightColor(Color.BLUE)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher)) // your app icon
                .build();

        telecomManager.registerPhoneAccount(phoneAccount);

        Log.d(TAG, "PhoneAccount registered: " + phoneAccount.isEnabled());
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
        notifyRemoteDeviceOfOutgoingCall(connection, userIdToCall, roomName, callerId, callerName);

        return connection;
    }

    // IMPORTANT: This network operation should NOT run on the main thread!
    // You need to perform this asynchronously.
    private void notifyRemoteDeviceOfOutgoingCall(VonageConnection conn, String remoteUserId, String roomName, String localUserId, String localUserName) {
        // For example, using a simple Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Send the FCM message to the remote device token(s)
                    // This usually involves making an HTTP POST request to the FCM v1 API
                    // This step is typically done from a secure backend using the Firebase Admin SDK!

                    // In a real app, you'd use Firebase Admin SDK on your backend to send the message
                    // You would NOT send FCM messages directly from the Android client in production this way.

                    // *temporary* access token (manually from OAuth Google Cloud Platform for testing)
                    String googleCloudToken = "ya29.c.c0ASRK0GaeFYiVHOO0nZbqcpFvIn86KX1jj8a-Xo86K7z8z3CpbkkLBI6Y4IrpXXfXTARLaGB8hj-P2lbeeSaMTSRPJpISfITAibjAuxRYApvFHsL7vECAJ4jG8rxn_QWW3rDoYySSeIBIGjVw9HjTeaAo1uHiFCjetouWYUe7o-MPfLdyRlibx4ZbWO8WNaftcOijrHohQhtlmFliyJRsGhj2rHvgDaiG1xj0Kd6xdmsnpIWHv6T78AN83Zl_dkpKONPtACbfM3HPTrSVMEXOtAoYkwuk4r-wYueP8SqJJCNfwCEDTASXHLf1rqytfMoHCYUiwF1GdCkxEeyYaWxySHmB6lsFeqDiZO-JIi9hZU5JyMGGVzRdY_ocN385Pwn66_2I7a-xgajq9taXqukJ0FfvvWnItn4WkqxvpU_q5OXe4V_-Xfvy8lMYJ6xbzWXUleO3wB2Jipfz_i3pfXoJax7xY9B0k5bsfS1mkJZi1zqxibsogkn74bZbRFz7r2UU1y_uSiIU6xy-xZg54hSn-0djFpivSufu6s3_YRBv-iiOz7gO_0Bm60dsO6uQhovQSjppgoxFbZIh0pmiyU12Xjzg0FXZ8m6YdJv3pmtVZUWJu-9Y0-f3gO1t6XwZ962kklWcUvsJ-55cQt2fXlYbQt16cilyWkhVfSgvazIFaxU6dOe6aJURu3Isstp_78VF5gUmM7nXWg7wm9unROzZvdtzV1Fzof4MjkJte4qhttOju8hazeXbFWSop4QRdQM18IQQQtfSaFdiVMOetW7k88QU046IwinVakYMz93QyBl5JFUchRfMkXV0UdYBpXivgmgwxc9pS99ypI5yvFdInpdbbxxhZcYh8xd7gzUuriVRUVi9R6jM3_S9v4wq72RM9ul7syeOlJrwRxxSYpMFIgs96JyhlbB2btzJM06kSVVFBbWsQj3f2lRbqlmhUZs26Vv5ilqafBBR2FtzRYlqbwU4kgZXl3gzbdqg4ssec2IR4szu0-i-Z3Q";

                    // Look up the remote user's FCM token(s) based on remoteUserId
                    String remoteDeviceFcmToken = lookupFcmTokenForUserId(remoteUserId);

                    if (!remoteDeviceFcmToken.isEmpty() && !googleCloudToken.isEmpty()) {
                        // Construct the FCM Json message payload
                        // You can pass any custom data as you want
                        JSONObject messageJson = new JSONObject();
                        JSONObject messageObject = new JSONObject();

                        messageObject.put("token", remoteDeviceFcmToken);

                        JSONObject notificationObject = new JSONObject();
                        notificationObject.put("title", "Incoming Call");
                        notificationObject.put("body", localUserName + " is calling..."); // Use actual caller name
                        messageObject.put("notification", notificationObject);

                        JSONObject dataObject = new JSONObject();
                        dataObject.put("type", "INCOMING_CALL");
                        dataObject.put("callerId", localUserId);
                        dataObject.put("callerName", localUserName);
                        dataObject.put("apiKey", API_KEY);
                        dataObject.put("sessionId", SESSION_ID);
                        dataObject.put("token", TOKEN);
                        messageObject.put("data", dataObject);

                        messageJson.put("message", messageObject);

                        String jsonBody = messageJson.toString();

                        // Construct the HTTP request
                        OkHttpClient client = new OkHttpClient(); // Use OkHttp, etc.
                        Request request = new Request.Builder()
                             .url("https://fcm.googleapis.com/v1/projects/vonageconnectionservice/messages:send")
                             .header("Content-Type", "application/json")
                             .header("Authorization", "Bearer " + googleCloudToken)
                             .header("X-GFE-SSL", "yes") // Often not strictly necessary, but good practice
                             .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                             .build();

                         Response response = client.newCall(request).execute();
                         if (response.isSuccessful()) {
                             Log.d(TAG, "FCM sent successfully from client: " + response.body().string());

                         } else {
                             Log.e(TAG, "FCM send failed from client: " + response.code() + " " + response.body().string());
                         }

                    } else {
                        Log.e(TAG, "FCM token or OAuth Google token not found");
                        // Handle error: maybe signal connection failure to Telecom
                        conn.setDisconnected(new DisconnectCause(DisconnectCause.ERROR, "User not found"));
                        conn.destroy();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error sending FCM notification:", e);
                    // Handle error: maybe signal connection failure to Telecom
                    conn.setDisconnected(new DisconnectCause(DisconnectCause.ERROR, "Failed to notify remote device"));
                    conn.destroy();
                }
            }
        }).start();
    }

    // This method needs to be implemented to fetch the token for a given user ID
    // This usually involves querying your app's backend or a database like Firestore
    // For now, you can return a hardcoded token for testing!
    private String lookupFcmTokenForUserId(String userId) {
        return "ekGT0O9gSu25QdgK07wSss:APA91bHpUFQ8hjSWd0GnA7zkfWXzu3z---DrIXKuQtpRn7ThxTkLldKdG5zDPot5gsgk7ZMWgiCYnaxvjCGW2SIcZkgzw2FYazltuZK5cSVGmVh3OX2AUCc";
    }

    // Dummy method to simulate FCM sending for testing purposes
    // In a real app, you'd use Firebase Admin SDK on your backend
    private boolean simulateFcmSend(String token, Map<String, String> data) {
        Log.d(TAG, "Simulating sending data payload to token: " + token + " with data: " + data);
        // In a real app, this would be an HTTP POST request to FCM endpoint or using Admin SDK
        // For a simulation, just assume success or failure for testing the flow
        return true; // Assume success for simulation
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

        notifyIncomingCall(callerName);

        return connection;
    }

    private void notifyIncomingCall(String callerName) {
        if (listener != null) {
            listener.onIncomingCall(callerName, "Incoming Call");
        }
    }
}