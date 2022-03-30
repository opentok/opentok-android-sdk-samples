package com.tokbox.sample.basicvoipcall;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private RelativeLayout publisherViewContainer;
    private LinearLayout subscriberViewContainer;


    private TelecomManager mTelecomManager;
    private TelephonyManager mTelephonyManager;
    private PhoneAccountHandle mPhoneAccountHandle;
    private PhoneAccount mPhoneAccount;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        //publisherViewContainer = findViewById(R.id.publisherview);
        //subscriberViewContainer = findViewById(R.id.subscriberview);

        requestPermissions();

        /*
        findViewById(R.id.call_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOutGoingCall();
            }
        });

         */

        findViewById(R.id.outgoing_call_button).setOnClickListener(clickListener);
        findViewById(R.id.incoming_call_button).setOnClickListener(clickListener);
        findViewById(R.id.register_button).setOnClickListener(clickListener);

        mTelecomManager = (TelecomManager) this.getSystemService(Context.TELECOM_SERVICE);
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        ComponentName componentName = new ComponentName(this, OTConnectionService.class);
        mPhoneAccountHandle = new PhoneAccountHandle(componentName, "VoIP Calling 1");

        mPhoneAccount = new PhoneAccount.Builder(mPhoneAccountHandle, "VoIP calling 2")
                //.setCapabilities(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .build();
    }

    @SuppressLint("NewApi")
    private void registerAccount() {
        mTelecomManager.registerPhoneAccount(mPhoneAccount);

        Intent intent=new Intent();
        intent.setComponent(new ComponentName("com.android.server.telecom",
                "com.android.server.telecom.settings.EnableAccountPreferenceActivity"));
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*
        if (session == null) {
            return;
        }

        session.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
         */
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        if (session == null) {
            return;
        }

        session.onResume();
         */
    }

    @Override
    protected void onDestroy() {
        //disconnectSession();
        super.onDestroy();
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
        String[] perms = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                //Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.MANAGE_OWN_CALLS,
                Manifest.permission.CALL_PHONE
        };

        if (EasyPermissions.hasPermissions(this, perms)) {
            //NoiseAudioDevice noiseAudioDevice = new NoiseAudioDevice(this);
            //AudioDeviceManager.setAudioDevice(noiseAudioDevice);

        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.outgoing_call_button:
                    startOutGoingCall();
                    break;

                case R.id.incoming_call_button:
                    startIncomingCall();
                    break;

                case R.id.register_button:
                    registerAccount();
                    break;

                default:
                    break;
            }
        }
    };

    @SuppressLint("NewApi")
    private void startIncomingCall() {
        Bundle callInfo = new Bundle();
        callInfo.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true);
        callInfo.putString("from", "+999999999999");

        Bundle test = new Bundle();
        test.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, mPhoneAccountHandle);
        test.putParcelable(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, callInfo);
        mTelecomManager.addNewIncomingCall(mPhoneAccountHandle, test);
    }

    @SuppressLint("NewApi")
    private void startOutGoingCall() {
        Log.i(TAG, "startOutGoingCall()");
        Bundle extras = new Bundle();
        extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true);
        //ComponentName componentName = new ComponentName(this, OTConnectionService.class);
        //PhoneAccountHandle phoneAccountHandle = new PhoneAccountHandle(componentName, "VoIP call");
        /*
        val receiver = call.callReceiver as User
        var number = receiver.statusMessage
        if (number.isNullOrEmpty())
            number = "09999999999"
        extras.putString("NAME",receiver.name)
        extras.putString("SESSIONID", call.sessionId)
        extras.putString("RECEIVERTYPE", call.receiverType)
        extras.putString("CALLTYPE", call.type)
        extras.putString("RECEIVERID",receiver.uid)
        if (call.receiverType.equals(CometChatConstants.RECEIVER_TYPE_GROUP, ignoreCase = true))
            extras.putString(UIKitConstants.IntentStrings.NAME, (call.receiver as Group).name)
        else
        extras.putString(UIKitConstants.IntentStrings.NAME, (call.callInitiator as User).name)

         */

        Bundle test = new Bundle();
        test.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, mPhoneAccountHandle);
        //test.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL);
        test.putParcelable(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mTelecomManager.placeCall(Uri.parse("tel:+00000000000"), test);
        /*
        try {
            if (callManagerContext.checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS)
                    == PackageManager.PERMISSION_GRANTED) {
                telecomManager.placeCall(Uri.parse("tel:$number"), test)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        */
    }
}
