package com.tokbox.sample.basicvideochat_connectionservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
    private NotificationChannelManager notificationChannelManager;

    private CallActionReceiver callActionReceiver = new CallActionReceiver();

    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private TextView callerNameTextView;
    private TextView callStatusTextView;
    private LinearLayout incomingCallLayout;
    private LinearLayout outgoingCallLayout;
    private LinearLayout endCallLayout;
    private LinearLayout devicesSelectorLayout;

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

    private final BroadcastReceiver callAnsweredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_ANSWERED_CALL.equals(intent.getAction())) {
                updateUIForAnsweredCall();
            }
        }
    };

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

        vonageManager = VonageManager.getInstance(getApplicationContext(), this);
        vonageManager.setAudioFocusManager(getApplicationContext());

        notificationChannelManager = new NotificationChannelManager(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannelManager.setup();
        }

        MyFirebaseMessagingService.setCallEventListener(this);

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);
        outgoingCallLayout = findViewById(R.id.outgoing_call_layout);
        callerNameTextView = findViewById(R.id.participantNameText);
        callStatusTextView = findViewById(R.id.callStatusText);
        incomingCallLayout = findViewById(R.id.incoming_call_layout);
        endCallLayout = findViewById(R.id.end_call_layout);
        devicesSelectorLayout = findViewById(R.id.audio_devices_layout);

        requestPermissions();

        registerCallActions();

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("simulate_incoming".equals(action)) {
                onIncomingCallButtonClick(null);
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerCallActions() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CallActionReceiver.ACTION_ANSWER_CALL);
        filter.addAction(CallActionReceiver.ACTION_REJECT_CALL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(callActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(callActionReceiver, filter);
        }


        LocalBroadcastManager.getInstance(this).registerReceiver(
                callAnsweredReceiver,
                new IntentFilter(CallActionReceiver.ACTION_ANSWERED_CALL)
        );
    }

    @Override
    public void onIncomingCall(String callerName, String callStatus) {
        runOnUiThread(() -> {
            if(Objects.equals(callStatus, "Call Cancelled")) {
                callerNameTextView.setText(callerName);
                callStatusTextView.setText(callStatus);
                outgoingCallLayout.setVisibility(View.VISIBLE);
                incomingCallLayout.setVisibility(View.INVISIBLE);
                devicesSelectorLayout.setVisibility(View.INVISIBLE);
                endCallLayout.setVisibility(View.INVISIBLE);
                publisherViewContainer.setVisibility(View.INVISIBLE);
            } else if(Objects.equals(callStatus, "Incoming Call")) {
                callerNameTextView.setText(callerName);
                callStatusTextView.setText(callStatus);
                outgoingCallLayout.setVisibility(View.GONE);
                incomingCallLayout.setVisibility(View.VISIBLE);
                devicesSelectorLayout.setVisibility(View.VISIBLE);
                endCallLayout.setVisibility(View.VISIBLE);
                publisherViewContainer.setVisibility(View.INVISIBLE);
            } else {
                callerNameTextView.setText(callerName);
                callStatusTextView.setText(callStatus);
                outgoingCallLayout.setVisibility(View.GONE);
                incomingCallLayout.setVisibility(View.INVISIBLE);
                devicesSelectorLayout.setVisibility(View.VISIBLE);
                endCallLayout.setVisibility(View.VISIBLE);
                publisherViewContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
        unregisterReceiver(callActionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(callAnsweredReceiver);
    }

    private void requestPermissions() {
        String[] perms;
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
        incomingCallLayout.setVisibility(View.INVISIBLE);
        FcmEventSender.getInstance().notifyCallerOfCallResponse(callerId, callerName, true);
        VonageManager.getInstance().getCurrentConnection().onAnswer();
    }

    public void onHangUpButtonClick(View view) {
        VonageManager.getInstance().endCall();
        incomingCallLayout.setVisibility(View.INVISIBLE);
        outgoingCallLayout.setVisibility(View.VISIBLE);
        devicesSelectorLayout.setVisibility(View.INVISIBLE);
        endCallLayout.setVisibility(View.INVISIBLE);
        publisherViewContainer.setVisibility(View.INVISIBLE);
        callStatusTextView.setText("");
        callerNameTextView.setText("");
    }

    public void onRejectIncomingCall(View view) {
        incomingCallLayout.setVisibility(View.INVISIBLE);
        outgoingCallLayout.setVisibility(View.VISIBLE);
        devicesSelectorLayout.setVisibility(View.INVISIBLE);
        endCallLayout.setVisibility(View.INVISIBLE);
        publisherViewContainer.setVisibility(View.INVISIBLE);
        callStatusTextView.setText("");
        callerNameTextView.setText("");
        FcmEventSender.getInstance().notifyCallerOfCallResponse(callerId, callerName, false);
        VonageManager.getInstance().getCurrentConnection().onReject();
    }

    public void onShowAudioDevicesButtonClick(View view) {
        AudioDeviceDialogFragment dialog = new AudioDeviceDialogFragment();
        dialog.show(getSupportFragmentManager(), "audio_device_dialog");
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
                publisherViewContainer.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void onIncomingCallButtonClick(View view) {
        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, PhoneAccountManager.getAccountHandle());
        extras.putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, "IdSimulatedCall");
        extras.putString("CALLER_NAME", "Simulated Caller");

        PhoneAccountManager.notifyIncomingVideoCall(extras);
    }

    public void onOutgoingCallButtonClick(View view) {
        PhoneAccountHandle handle = PhoneAccountManager.getAccountHandle();

        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        extras.putBoolean(TelecomManager.METADATA_IN_CALL_SERVICE_UI, true);
        extras.putString("CALLER_NAME", "Simulated Caller");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VonageManager.getInstance().setOnConnectionReadyListener(connection -> {
                connection.onPlaceCall(); // Answer after connection is ready
                onIncomingCall("Simulated Caller", "In call");
            });
            PhoneAccountManager.startOutgoingVideoCall(this, extras);
        }
    }

    public void onSimulateIncomingCallButtonClick(View view) {
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
        publisherViewContainer.setVisibility(View.VISIBLE);
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
        VonageManager.getInstance().endCall();
    }

    private void updateUIForAnsweredCall() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.INVISIBLE);
            outgoingCallLayout.setVisibility(View.INVISIBLE);
            devicesSelectorLayout.setVisibility(View.VISIBLE);
            endCallLayout.setVisibility(View.VISIBLE);
            publisherViewContainer.setVisibility(View.VISIBLE);
            callStatusTextView.setText("In Call");
            callerNameTextView.setText(callerName);
        });
    }
}
