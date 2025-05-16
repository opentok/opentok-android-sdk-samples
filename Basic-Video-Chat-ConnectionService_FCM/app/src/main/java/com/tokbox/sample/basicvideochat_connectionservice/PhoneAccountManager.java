package com.tokbox.sample.basicvideochat_connectionservice;

import static android.telecom.PhoneAccount.EXTRA_LOG_SELF_MANAGED_CALLS;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import java.util.Collections;

public class PhoneAccountManager {

    public static final String ACCOUNT_ID = "vonage_video_call";
    private static PhoneAccount phoneAccount;
    private static PhoneAccountHandle handle;
    private static TelecomManager telecomManager;

    public static TelecomManager getTelecomManager() {
        return telecomManager;
    }
    public static PhoneAccountHandle getAccountHandle() {
        return handle;
    }

    public static void registerPhoneAccount(Context context) {
        telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

        ComponentName componentName = new ComponentName(context, VonageConnectionService.class);
        handle = new PhoneAccountHandle(componentName, ACCOUNT_ID);

        phoneAccount = PhoneAccount.builder(handle, "Vonage Video")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .setSupportedUriSchemes(Collections.singletonList("vonagecall"))
                .setHighlightColor(Color.BLUE)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .build();

        telecomManager.registerPhoneAccount(phoneAccount);

        Log.d("PhoneAccountManager", "PhoneAccount registered: " + phoneAccount.isEnabled());
    }
}
