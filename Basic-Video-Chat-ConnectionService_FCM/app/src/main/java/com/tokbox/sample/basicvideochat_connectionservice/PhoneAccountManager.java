package com.tokbox.sample.basicvideochat_connectionservice;

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

public class PhoneAccountManager {

    public static final String ACCOUNT_ID = "vonage_video_call";
    private static String VONAGE_CALL_SCHEME = "vonagecall";
    public PhoneAccountHandle handle;
    private TelecomManager telecomManager;
    private final Context context;

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

    public void startOutgoingVideoCall(Context context, Bundle extras) {
        extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_BIDIRECTIONAL);

        Uri calleeUri = Uri.fromParts(VONAGE_CALL_SCHEME, "user-42", null);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        telecomManager.placeCall(calleeUri, extras);
    }

    public void notifyIncomingVideoCall(Bundle extras) {
        if (telecomManager != null && handle != null) {
            telecomManager.addNewIncomingCall(handle, extras);
            Log.d("PhoneAccountManager", "Incoming video call notified.");
        } else {
            Log.e("PhoneAccountManager", "TelecomManager or PhoneAccountHandle is null. Cannot notify incoming call.");
        }
    }
}
