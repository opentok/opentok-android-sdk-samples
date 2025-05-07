package com.tokbox.sample.basicvideochat_connectionservice;

import static android.telecom.TelecomManager.PRESENTATION_ALLOWED;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import androidx.annotation.NonNull;

public class VonageConnection extends Connection {

    private final Context context;
    private String mRoomName;
    private Intent mLaunchIntent;
    private static final int REQUEST_CODE_ROOM_ACTIVITY = 2;

    public VonageConnection(@NonNull Context context, String roomName, String callerId, String callerName) {
        this.context = context;
        this.mRoomName = roomName;

        setCallerDisplayName(callerName, PRESENTATION_ALLOWED);
        setAddress(Uri.fromParts("tel", callerId, null), TelecomManager.PRESENTATION_ALLOWED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // If using Phone Call UI
            setConnectionProperties(PROPERTY_SELF_MANAGED);
        }

        setAudioModeIsVoip(true);

        int capabilities = CAPABILITY_HOLD | CAPABILITY_SUPPORT_HOLD | CAPABILITY_MUTE;
        setConnectionCapabilities(capabilities);

    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        setActive();

    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        destroy();
    }

    @Override
    public void onReject() {
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        destroy();
    }

    @Override
    public void onHold() {
        super.onHold();
        setOnHold();
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
        setActive();
    }
}
