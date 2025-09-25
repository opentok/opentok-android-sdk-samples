package com.tokbox.sample.basicvideochat_connectionservice;

import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.tokbox.sample.basicvideochat_connectionservice.network.APIService;
import com.tokbox.sample.basicvideochat_connectionservice.network.GetSessionResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements VonageSessionListener, CallEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 1234;

    private Retrofit retrofit;
    private APIService apiService;

    private VonageManager vonageManager;
    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private Button makeFcmCallButton;
    private Button makeSimulatedCallButton;
    private TextView callerNameTextView;
    private TextView callStatusTextView;
    private LinearLayout incomingCallLayout;
    private LinearLayout outgoingCallLayout;

    // These values should be passed by the user
    // Based on the id, your server should retrieve the corresponding FCM Token
    // and with it, you can use FCM to send a notification push to that client
    private String callerId = "123456";
    private String callerName = "Mom";

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    String permission = entry.getKey();
                    Boolean isGranted = entry.getValue();
                    Log.d("Permission", permission + " -> " + (isGranted ? "GRANTED" : "DENIED"));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register Firebase client to create unique ID
        FirebaseApp.initializeApp(this);
        PhoneAccountManager.registerPhoneAccount(this);

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                String token = task.getResult();
                Log.d("FCM", "Firebase Token: " + token);

                // Send token to your app server
            });

        vonageManager = VonageManager.getInstance(this, this);
        MyFirebaseMessagingService.setCallEventListener(this);

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);
        makeFcmCallButton = findViewById(R.id.button_call_fcm);
        makeSimulatedCallButton = findViewById(R.id.button_call_simulate);
        outgoingCallLayout = findViewById(R.id.outgoing_call_layout);
        callerNameTextView = findViewById(R.id.participantNameText);
        callStatusTextView = findViewById(R.id.callStatusText);
        incomingCallLayout = findViewById(R.id.incoming_call_layout);

        requestPermissions();
    }

    @Override
    public void onIncomingCall(String callerName, String callStatus) {
        runOnUiThread(() -> {
            if(Objects.equals(callStatus, "Call Cancelled")) {
                callerNameTextView.setText(callerName);
                callStatusTextView.setText(callStatus);
                outgoingCallLayout.setVisibility(View.VISIBLE);
                incomingCallLayout.setVisibility(View.INVISIBLE);
            } else if(Objects.equals(callStatus, "Incoming Call")) {
                callerNameTextView.setText(callerName);
                callStatusTextView.setText(callStatus);
                outgoingCallLayout.setVisibility(View.GONE);
                incomingCallLayout.setVisibility(View.VISIBLE);
            } else {
                callerNameTextView.setText(callerName);
                callStatusTextView.setText(callStatus);
                outgoingCallLayout.setVisibility(View.GONE);
                incomingCallLayout.setVisibility(View.INVISIBLE);
            }
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

    private void requestPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{Manifest.permission.INTERNET,
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.MANAGE_OWN_CALLS,
                                Manifest.permission.FOREGROUND_SERVICE};
        } else {
            perms = new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CALL_PHONE};
        }

        boolean allPermissionsGranted = true;
        for (String permission : perms) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if(!allPermissionsGranted) {
            permissionLauncher.launch(perms);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (!isBatteryOptimizationIgnored()) {
                requestBatteryOptimizationIntent();
            }
        }
    }

    /**
     * Checks if battery optimizations are ignored for the app.
     *
     * @return true if the app is ignoring battery optimizations, false otherwise.
     */
    boolean isBatteryOptimizationIgnored() {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
    }

    /**
     * Setting Battery to Ignore Optimizations will prevent on Android 15 or above to disable network
     * connectivity when app is on background
     * +info: https://developer.android.com/reference/android/provider/Settings#ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
     *
     * @return The Intent to request battery optimization exemption, or null if already ignored.
     */
    void requestBatteryOptimizationIntent() {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        String packageName = getApplicationContext().getPackageName();

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    public void onAcceptIncomingCall(View view) {
        incomingCallLayout.setVisibility(View.INVISIBLE);
        FcmEventSender.getInstance().notifyCallerOfCallResponse(callerId, callerName, true);
        VonageManager.getInstance().getCurrentConnection().onAnswer();
    }

    public void onRejectIncomingCall(View view) {
        incomingCallLayout.setVisibility(View.INVISIBLE);
        outgoingCallLayout.setVisibility(View.VISIBLE);
        callStatusTextView.setText("");
        callerNameTextView.setText("");
        FcmEventSender.getInstance().notifyCallerOfCallResponse(callerId, callerName, false);
        VonageManager.getInstance().getCurrentConnection().onReject();
    }

    // This is a showcase of how to handle a outgoing call
    // This usually involves making an HTTP POST request to the FCM v1 API
    // which based on the provided IDs, it will get the corresponding FCM token
    // of the remote device token(s) to message.
    // Here we are populating the 
    public void onCallButtonClick(View view) {
        // Use hardcoded session config
        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        if( PhoneAccountManager.getTelecomManager() != null && PhoneAccountManager.getAccountHandle() != null) {
            Bundle extras = new Bundle();
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, PhoneAccountManager.getAccountHandle());

            // Build the URI with custom data in query parameters
            Uri destinationUri = Uri.fromParts("vonagecall", callerId, null).buildUpon()
                    .appendQueryParameter("callerId", callerId)
                    .appendQueryParameter("callerName", callerName)
                    .build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED) {
                PhoneAccountManager.getTelecomManager().placeCall(destinationUri, extras);

                // Update UI
                callStatusTextView.setText("Calling");
                callerNameTextView.setText(callerName);
                outgoingCallLayout.setVisibility(View.INVISIBLE);
                incomingCallLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void onSimulatedCallButtonClick(View view) {
        PhoneAccountHandle handle = PhoneAccountManager.getAccountHandle();

        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        extras.putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, "IdSimulatedCall");
        extras.putBoolean(TelecomManager.METADATA_IN_CALL_SERVICE_UI, true);
        extras.putString("CALLER_NAME", "Simulated Caller");

        TelecomManager telecomManager = PhoneAccountManager.getTelecomManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(telecomManager.isIncomingCallPermitted(handle)) {
                VonageManager.getInstance().setOnConnectionReadyListener(connection -> {
                    connection.onAnswer(); // Answer after connection is ready
                    onIncomingCall("Simulated Caller", "In call");
                });
                telecomManager.addNewIncomingCall(handle, extras);
            }
        }
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
