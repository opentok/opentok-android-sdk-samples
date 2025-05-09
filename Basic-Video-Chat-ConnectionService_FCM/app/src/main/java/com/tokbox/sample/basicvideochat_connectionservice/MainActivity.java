package com.tokbox.sample.basicvideochat_connectionservice;

import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.tokbox.sample.basicvideochat_connectionservice.R;
import com.tokbox.sample.basicvideochat_connectionservice.network.APIService;
import com.tokbox.sample.basicvideochat_connectionservice.network.GetSessionResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, VonageSessionListener, CallEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Retrofit retrofit;
    private APIService apiService;

    private VonageManager vonageManager;
    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private ImageButton makeCallButton;
    private TextView callerNameTextView;
    private TextView callStatusTextView;
    private LinearLayout incomingCallLayout;

    private static PhoneAccount phoneAccount;
    private static PhoneAccountHandle handle;
    private static TelecomManager telecomManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register Firebase client to create unique ID
        FirebaseApp.initializeApp(this);
        VonageConnectionService.registerPhoneAccount(this);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM", "Firebase Token: " + token);

                    // Send token to your app server
                    // To place a call to a client, you need its ID
                });

        vonageManager = VonageManager.getInstance(this, this);
        VonageConnectionService.setCallEventListener(this);

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);
        makeCallButton = findViewById(R.id.call);
        callerNameTextView = findViewById(R.id.participantNameText);
        callStatusTextView = findViewById(R.id.callStatusText);
        incomingCallLayout = findViewById(R.id.incoming_call_controls);

        requestPermissions();
    }

    @Override
    public void onIncomingCall(String callerName, String callStatus) {
        runOnUiThread(() -> {
            callerNameTextView.setText(callerName);
            callStatusTextView.setText(callStatus);
            makeCallButton.setVisibility(View.GONE);
            incomingCallLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        vonageManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        vonageManager.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        vonageManager.endSession();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ": " + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        String[] perms = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{Manifest.permission.INTERNET,
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.MANAGE_OWN_CALLS};
        } else {
            perms = new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CALL_PHONE};
        }

        if (EasyPermissions.hasPermissions(this, perms)) {

            if (ServerConfig.hasChatServerUrl()) {
                // Custom server URL exists - retrieve session config
                if(!ServerConfig.isValid()) {
                    finishWithMessage("Invalid chat server url: " + ServerConfig.CHAT_SERVER_URL);
                    return;
                }

                initRetrofit();
                getSession();
            } else {

            }
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }

        /*
        // The user needs to grant app to place calls
        phoneAccount = VonageConnectionService.getPhoneAccount();
        if (!phoneAccount.isEnabled()) {
            showEnableAccountPrompt();
        }

         */

    }

    /* Make a request for session data */
    private void getSession() {
        Log.i(TAG, "getSession");

        Call<GetSessionResponse> call = apiService.getSession();

        call.enqueue(new Callback<GetSessionResponse>() {
            @Override
            public void onResponse(Call<GetSessionResponse> call, Response<GetSessionResponse> response) {
                GetSessionResponse body = response.body();
                vonageManager.initializeSession(body.apiKey, body.sessionId, body.token);
            }

            @Override
            public void onFailure(Call<GetSessionResponse> call, Throwable t) {
                throw new RuntimeException(t.getMessage());
            }
        });
    }

    public void onAcceptIncomingCall(View view) {
        VonageManager.getInstance().initializeSession(API_KEY, SESSION_ID, TOKEN);
    }

    public void onRejectIncomingCall(View view) {
        VonageManager.getInstance().endSession();
    }

    public void onCallButtonClick(View view) {
        // Use hardcoded session config
        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        telecomManager = VonageConnectionService.getTelecomManager();
        handle = VonageConnectionService.getAccountHandle();

        if( telecomManager != null && handle != null) {
            Bundle extras = new Bundle();
            //extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
            String userIdToCall = "momdevice";
            String roomName = "room_xyz";
            String callerId = "user123";
            String callerName = "Mom";

            // Build the URI with custom data in query parameters
            Uri destinationUri = Uri.fromParts("vonagecall", userIdToCall, null).buildUpon()
                    .appendQueryParameter("roomName", roomName)
                    .appendQueryParameter("callerId", callerId)
                    .appendQueryParameter("callerName", callerName)
                    .build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED) {
                telecomManager.placeCall(destinationUri, extras);
                VonageManager.getInstance().initializeSession(API_KEY, SESSION_ID, TOKEN);
                makeCallButton.setVisibility(View.INVISIBLE);
                incomingCallLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showEnableAccountPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Calling")
                .setMessage("To receive calls, please enable call permissions.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.CHAT_SERVER_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(APIService.class);
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    @Override
    public void onPublisherViewReady(View view) {
        publisherViewContainer.addView(view);
    }

    @Override
    public void onSubscriberViewReady(View view) {
        subscriberViewContainer.addView(view);
    }

    @Override
    public void onStreamDropped() {
        subscriberViewContainer.removeAllViews();
    }

    @Override
    public void onError(String message) {
        finishWithMessage(message);
    }
}
