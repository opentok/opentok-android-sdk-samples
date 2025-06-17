package com.tokbox.sample.basicvideochat_connectionservice.connectionservice;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.tokbox.sample.basicvideochat_connectionservice.R;

public class PhoneAccountManager {

    public static final String ACCOUNT_ID = "vonage_video_call";
    private static String VONAGE_CALL_SCHEME = "vonagecall";
    public PhoneAccountHandle handle;
    private TelecomManager telecomManager;
    private final Context context;
    public static String CALLER_NAME = "CALLER_NAME";
    public static String CALLER_ID = "CALLER_ID";

    public PhoneAccountManager(Context context) {
        this.context = context;
    }

    public void registerPhoneAccount() {
        telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

        ComponentName componentName = new ComponentName(context, VonageConnectionService.class);
        handle = new PhoneAccountHandle(componentName, ACCOUNT_ID);

        PhoneAccount phoneAccount;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            phoneAccount = PhoneAccount.builder(handle, "Vonage Video")
                    .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED | PhoneAccount.CAPABILITY_VIDEO_CALLING)
                    .setHighlightColor(Color.BLUE)
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .addSupportedUriScheme(VONAGE_CALL_SCHEME)
                    .build();
        } else {
            phoneAccount = PhoneAccount.builder(handle, "Vonage Video")
                    .setCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)
                    .setHighlightColor(Color.BLUE)
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .addSupportedUriScheme(VONAGE_CALL_SCHEME)
                    .build();
        }

        telecomManager.registerPhoneAccount(phoneAccount);
        Log.d("PhoneAccountManager", "PhoneAccount registered: " + phoneAccount.isEnabled());
    }

    public void startOutgoingVideoCall(String callerName, String callerId) {
        Bundle extras = new Bundle();
        extras.putString(PhoneAccountManager.CALLER_NAME, callerName);
        extras.putString(PhoneAccountManager.CALLER_ID, callerId);
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_BIDIRECTIONAL);

        Uri calleeUri = new Uri.Builder()
                .scheme(VONAGE_CALL_SCHEME)
                .authority(callerId)
                .appendQueryParameter("callerName", callerName)
                .build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        telecomManager.placeCall(calleeUri, extras);
    }

    public void notifyIncomingVideoCall(String callerName, String callerId) {
        Bundle extras = new Bundle();
        extras.putString(PhoneAccountManager.CALLER_NAME, callerName);
        extras.putString(PhoneAccountManager.CALLER_ID, callerId);
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);

        Uri calleeUri = new Uri.Builder()
                .scheme(VONAGE_CALL_SCHEME)
                .authority(callerId)
                .appendQueryParameter("callerName", callerName)
                .build();

        extras.putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, calleeUri.toString());

        if (telecomManager != null && handle != null) {
            telecomManager.addNewIncomingCall(handle, extras);
            Log.d("PhoneAccountManager", "Incoming video call notified.");
        } else {
            Log.e("PhoneAccountManager", "TelecomManager or PhoneAccountHandle is null. Cannot notify incoming call.");
        }
    }

    public boolean canPlaceIncomingCall() {
        if (telecomManager == null || handle == null) {
            Log.e("PhoneAccountManager", "TelecomManager or PhoneAccountHandle is not initialized.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telecomManager.isIncomingCallPermitted(handle);
        } else {
            return true;
        }
    }

    public boolean canPlaceOutgoingCall() {
        if (telecomManager == null || handle == null) {
            Log.e("PhoneAccountManager", "TelecomManager or PhoneAccountHandle is not initialized.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telecomManager.isOutgoingCallPermitted(handle);
        } else {
            return true;
        }
    }
}
