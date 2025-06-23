package com.tokbox.sample.basicvideochatconnectionservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ComponentCaller;
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

import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.PhoneAccountManager;
import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.VonageConnection;
import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.VonageConnectionHolder;
import com.tokbox.sample.basicvideochatconnectionservice.deviceselector.AudioDeviceDialogFragment;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements VonageSessionListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private VonageManager vonageManager;
    private NotificationChannelManager notificationChannelManager;
    private PhoneAccountManager phoneAccountManager;
    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private TextView callerNameTextView;
    private TextView callStatusTextView;
    private LinearLayout incomingCallLayout;
    private LinearLayout outgoingCallLayout;
    private LinearLayout endCallLayout;
    private LinearLayout devicesSelectorLayout;
    private Button buttonIsOnHold;

    private LocalBroadcastManager localBroadcastManager;

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
                String callerName = intent.getStringExtra(PhoneAccountManager.CALLER_NAME);
                showOngoingCall(callerName);
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

                String callerName = intent.getStringExtra(PhoneAccountManager.CALLER_NAME);
                String callerId = intent.getStringExtra(PhoneAccountManager.CALLER_ID);
                launchIncomingCall(callerName, callerId);
            }
        }
    };

    private final BroadcastReceiver holdingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_CALL_HOLDING.equals(intent.getAction())) {
                showHolding();
            }
        }
    };

    private final BroadcastReceiver unHoldingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallActionReceiver.ACTION_CALL_UNHOLDING.equals(intent.getAction())) {
                showUnHolding();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vonageManager = VonageManager.getInstance(getApplicationContext(), this);
        vonageManager.setAudioFocusManager(getApplicationContext());
        phoneAccountManager = new PhoneAccountManager(this);
        phoneAccountManager.registerPhoneAccount();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

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
        buttonIsOnHold = findViewById(R.id.button_is_on_hold);

        requestPermissions();

        registerCallActions();

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("simulate_incoming".equals(action)) {

                String callerName = intent.getStringExtra(PhoneAccountManager.CALLER_NAME);
                String callerId = intent.getStringExtra(PhoneAccountManager.CALLER_ID);
                launchIncomingCall(callerName, callerId);
            }
        }

        // Launches the Phone Account settings screen to allow the user to manage phone accounts.
        // startActivity(new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS));
    }

    @Override
    public void onNewIntent(@NonNull Intent intent, @NonNull ComponentCaller caller) {
        super.onNewIntent(intent, caller);

        String action = intent.getStringExtra("action");
        if ("simulate_incoming".equals(action)) {

            String callerName = intent.getStringExtra(PhoneAccountManager.CALLER_NAME);
            String callerId = intent.getStringExtra(PhoneAccountManager.CALLER_ID);
            launchIncomingCall(callerName, callerId);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerCallActions() {
        registerReceiver(callAnsweredReceiver, CallActionReceiver.ACTION_ANSWERED_CALL);
        registerReceiver(incomingCallReceiver, CallActionReceiver.ACTION_INCOMING_CALL);
        registerReceiver(rejectedIncomingCallReceiver, CallActionReceiver.ACTION_REJECTED_CALL);
        registerReceiver(callEndedReceiver, CallActionReceiver.ACTION_CALL_ENDED);
        registerReceiver(incomingNotificationReceiver, CallActionReceiver.ACTION_NOTIFY_INCOMING_CALL);
        registerReceiver(holdingReceiver, CallActionReceiver.ACTION_CALL_HOLDING);
        registerReceiver(unHoldingReceiver, CallActionReceiver.ACTION_CALL_UNHOLDING);
    }

    private void registerReceiver(BroadcastReceiver receiver, String action) {
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(action));
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
        localBroadcastManager.unregisterReceiver(callAnsweredReceiver);
        localBroadcastManager.unregisterReceiver(incomingCallReceiver);
        localBroadcastManager.unregisterReceiver(rejectedIncomingCallReceiver);
        localBroadcastManager.unregisterReceiver(callEndedReceiver);
        localBroadcastManager.unregisterReceiver(incomingNotificationReceiver);
        localBroadcastManager.unregisterReceiver(holdingReceiver);
        localBroadcastManager.unregisterReceiver(unHoldingReceiver);

        localBroadcastManager = null;
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
        invokeIntent(CallActionReceiver.ACTION_ANSWER_CALL);
    }

    public void onRejectIncomingCall(View view) {
        invokeIntent(CallActionReceiver.ACTION_REJECT_CALL);
    }

    public void onHangUpButtonClick(View view) {
        invokeIntent(CallActionReceiver.ACTION_END_CALL);
        resetCallLayout();
    }

    public void invokeIntent(String action) {
        Intent endIntent = new Intent(action);
        endIntent.setPackage(getPackageName());
        sendBroadcast(endIntent);
    }

    public void onShowAudioDevicesButtonClick(View view) {
        AudioDeviceDialogFragment dialog = new AudioDeviceDialogFragment();
        dialog.show(getSupportFragmentManager(), "audio_device_dialog");
    }

    public void onIncomingCallButtonClick(View view) {
        String callerName = "Simulated Caller";
        String callerId = "+4401539702257";

        launchIncomingCall(callerName, callerId);
    }

    private void launchIncomingCall(String callerName, String callerId) {
        if (phoneAccountManager.canPlaceIncomingCall()) {
            phoneAccountManager.notifyIncomingVideoCall(callerName, callerId);
        }
    }

    public void onOutgoingCallButtonClick(View view) {
        String callerName = "Simulated Caller";
        String callerId = "+4401539702257";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && phoneAccountManager.canPlaceOutgoingCall()) {
            phoneAccountManager.startOutgoingVideoCall(callerName, callerId);
            showOngoingCall(callerName);
        }
    }

    public void onUnHoldButtonClick(View view) {
        VonageConnection connection = VonageConnectionHolder.getInstance().getConnection();
        if (connection != null) {
            connection.onUnhold();
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
            buttonIsOnHold.setVisibility(View.INVISIBLE);
            onStreamDropped();
        });
    }

    private void showOngoingCall(String remoteName) {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.INVISIBLE);
            outgoingCallLayout.setVisibility(View.INVISIBLE);
            devicesSelectorLayout.setVisibility(View.VISIBLE);
            endCallLayout.setVisibility(View.VISIBLE);
            publisherViewContainer.setVisibility(View.VISIBLE);
            callStatusTextView.setText("In call");
            callerNameTextView.setText(remoteName);
            buttonIsOnHold.setVisibility(View.INVISIBLE);
        });
    }

    private void showIncomingCallLayout() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.VISIBLE);
            outgoingCallLayout.setVisibility(View.INVISIBLE);
            devicesSelectorLayout.setVisibility(View.INVISIBLE);
            endCallLayout.setVisibility(View.INVISIBLE);
            publisherViewContainer.setVisibility(View.INVISIBLE);
            buttonIsOnHold.setVisibility(View.INVISIBLE);
        });
    }

    private void hideIncomingCallLayout() {
        runOnUiThread(() -> {
            incomingCallLayout.setVisibility(View.INVISIBLE);
            outgoingCallLayout.setVisibility(View.VISIBLE);
            devicesSelectorLayout.setVisibility(View.INVISIBLE);
            endCallLayout.setVisibility(View.INVISIBLE);
            publisherViewContainer.setVisibility(View.INVISIBLE);
            onStreamDropped();
        });
    }

    private void showHolding() {
        runOnUiThread(() -> {
            callStatusTextView.setText("On Hold");
            buttonIsOnHold.setVisibility(View.VISIBLE);
        });
    }

    private void showUnHolding() {
        runOnUiThread(() -> {
            callStatusTextView.setText("In call");
            buttonIsOnHold.setVisibility(View.INVISIBLE);
        });
    }
}
