package com.tokbox.sample.basicvideochat_connectionservice.ui;

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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.tokbox.sample.basicvideochat_connectionservice.CallActionReceiver;
import com.tokbox.sample.basicvideochat_connectionservice.NotificationChannelManager;
import com.tokbox.sample.basicvideochat_connectionservice.R;
import com.tokbox.sample.basicvideochat_connectionservice.VonageManager;
import com.tokbox.sample.basicvideochat_connectionservice.VonageSessionListener;
import com.tokbox.sample.basicvideochat_connectionservice.connectionservice.PhoneAccountManager;
import com.tokbox.sample.basicvideochat_connectionservice.deviceselector.AudioDeviceDialogFragment;

import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements VonageSessionListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private VonageManager vonageManager;
    private NotificationChannelManager notificationChannelManager;
    private PhoneAccountManager phoneAccountManager;

    private CallActionReceiver callActionReceiver = new CallActionReceiver();

    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private TextView callerNameTextView;
    private TextView callStatusTextView;
    private LinearLayout incomingCallLayout;
    private LinearLayout outgoingCallLayout;
    private LinearLayout endCallLayout;
    private LinearLayout devicesSelectorLayout;

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

    private final BroadcastReceiver incomingCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_INCOMING_CALL.equals(intent.getAction())) {
                showIncomingCallLayout();
            }
        }
    };

    private final BroadcastReceiver rejectedIncomingCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_REJECTED_CALL.equals(intent.getAction())) {
                hideIncomingCallLayout();
            }
        }
    };

    private final BroadcastReceiver callEndedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_CALL_ENDED.equals(intent.getAction())) {
                resetCallLayout();
            }
        }
    };

    private final BroadcastReceiver incomingNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_NOTIFY_INCOMING_CALL.equals(intent.getAction())) {
                launchIncomingCall();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register Firebase client to create unique ID
        FirebaseApp.initializeApp(this);

        phoneAccountManager = new PhoneAccountManager(this);
        phoneAccountManager.registerPhoneAccount();

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

        LocalBroadcastManager.getInstance(this).registerReceiver(
                incomingCallReceiver,
                new IntentFilter(CallActionReceiver.ACTION_INCOMING_CALL)
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                rejectedIncomingCallReceiver,
                new IntentFilter(CallActionReceiver.ACTION_REJECTED_CALL)
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                callEndedReceiver,
                new IntentFilter(CallActionReceiver.ACTION_CALL_ENDED)
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                incomingNotificationReceiver,
                new IntentFilter(CallActionReceiver.ACTION_NOTIFY_INCOMING_CALL)
        );
    }

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(incomingCallReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(rejectedIncomingCallReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(callEndedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(incomingNotificationReceiver);
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

    public void onAcceptIncomingCall(View view) {
        Intent answerIntent = new Intent(CallActionReceiver.ACTION_ANSWER_CALL);
        answerIntent.setPackage(getPackageName());
        sendBroadcast(answerIntent);
    }

    public void onRejectIncomingCall(View view) {
        Intent rejectIntent = new Intent(CallActionReceiver.ACTION_REJECT_CALL);
        rejectIntent.setPackage(getPackageName());
        sendBroadcast(rejectIntent);
    }

    public void onHangUpButtonClick(View view) {
        Intent endIntent = new Intent(CallActionReceiver.ACTION_END_CALL);
        endIntent.setPackage(getPackageName());
        sendBroadcast(endIntent);

        incomingCallLayout.setVisibility(View.INVISIBLE);
        outgoingCallLayout.setVisibility(View.VISIBLE);
        devicesSelectorLayout.setVisibility(View.INVISIBLE);
        endCallLayout.setVisibility(View.INVISIBLE);
        publisherViewContainer.setVisibility(View.INVISIBLE);
        callStatusTextView.setText("");
        callerNameTextView.setText("");
    }

    public void onShowAudioDevicesButtonClick(View view) {
        AudioDeviceDialogFragment dialog = new AudioDeviceDialogFragment();
        dialog.show(getSupportFragmentManager(), "audio_device_dialog");
    }

    public void onIncomingCallButtonClick(View view) {
        launchIncomingCall();
    }

    private void launchIncomingCall() {
        Bundle extras = new Bundle();
        extras.putString("CALLER_NAME", "Simulated Caller");

        phoneAccountManager.notifyIncomingVideoCall(extras);
    }

    public void onOutgoingCallButtonClick(View view) {
        PhoneAccountHandle handle = phoneAccountManager.handle;

        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        extras.putString("CALLER_NAME", "Simulated Caller");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            phoneAccountManager.startOutgoingVideoCall(this, extras);
            onIncomingCall("Simulated Caller", "In call");
        }
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

    private void resetCallLayout() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.INVISIBLE);
            outgoingCallLayout.setVisibility(View.VISIBLE);
            devicesSelectorLayout.setVisibility(View.INVISIBLE);
            endCallLayout.setVisibility(View.INVISIBLE);
            publisherViewContainer.setVisibility(View.INVISIBLE);
            callStatusTextView.setText("");
            callerNameTextView.setText("");
        });
    }

    private void updateUIForAnsweredCall() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.INVISIBLE);
            outgoingCallLayout.setVisibility(View.INVISIBLE);
            devicesSelectorLayout.setVisibility(View.VISIBLE);
            endCallLayout.setVisibility(View.VISIBLE);
            publisherViewContainer.setVisibility(View.VISIBLE);
            callStatusTextView.setText("In Call");
            callerNameTextView.setText("Mom");
        });
    }

    private void showIncomingCallLayout() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.VISIBLE);
            outgoingCallLayout.setVisibility(View.INVISIBLE);
            devicesSelectorLayout.setVisibility(View.INVISIBLE);
            endCallLayout.setVisibility(View.INVISIBLE);
            publisherViewContainer.setVisibility(View.INVISIBLE);
        });
    }

    private void hideIncomingCallLayout() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.INVISIBLE);
            outgoingCallLayout.setVisibility(View.VISIBLE);
            devicesSelectorLayout.setVisibility(View.INVISIBLE);
            endCallLayout.setVisibility(View.INVISIBLE);
            publisherViewContainer.setVisibility(View.INVISIBLE);
        });
    }
}
