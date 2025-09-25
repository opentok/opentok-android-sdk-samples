package com.tokbox.sample.basicvideochat_connectionservice;

import static android.telecom.TelecomManager.PRESENTATION_ALLOWED;

import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.API_KEY;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.SESSION_ID;
import static com.tokbox.sample.basicvideochat_connectionservice.OpenTokConfig.TOKEN;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.annotation.NonNull;

public class VonageConnection extends Connection {

    private final Context context;
    private String mRoomName;
    private Intent mLaunchIntent;
    private static final int REQUEST_CODE_ROOM_ACTIVITY = 2;
    private String mApiKey = "";
    private String mSessionId = "";
    private String mToken = "";
    private AdvancedAudioDevice advancedAudioDevice;
    private boolean automaticHandlingAudioFocus = false;
    boolean initialised = false;

    public VonageConnection(@NonNull Context context, String apiKey, String sessionId, String token, String callerId, String callerName) {
        this.context = context;
        this.mApiKey = apiKey;
        this.mSessionId = sessionId;
        this.mToken = token;

        advancedAudioDevice = new AdvancedAudioDevice(context, automaticHandlingAudioFocus); // passing true fails because of connection service

        setCallerDisplayName(callerName, PRESENTATION_ALLOWED);
        setAddress(Uri.fromParts("vonagecall", callerId, null), TelecomManager.PRESENTATION_ALLOWED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            setConnectionProperties(PROPERTY_SELF_MANAGED);
        }

        setAudioModeIsVoip(true);

        int capabilities = CAPABILITY_HOLD | CAPABILITY_SUPPORT_HOLD | CAPABILITY_MUTE;
        setConnectionCapabilities(capabilities);

    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        Log.d("VonageConnection", "Audio state is " + audioState.toString());
    }

    public void onPlaceCall() {
        setActive();
        VonageManager.getInstance().initializeSession(advancedAudioDevice, API_KEY, SESSION_ID, TOKEN);
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        setActive();
        if(!initialised) {
            VonageManager.getInstance().initializeSession(advancedAudioDevice, API_KEY, SESSION_ID, TOKEN);
            initialised = true;
        }
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        VonageManager.getInstance().endSession();
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

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);

        if (state == Connection.STATE_ACTIVE) {
            if(!automaticHandlingAudioFocus) {
                advancedAudioDevice.requestAudioFocus();
            }
        }
    }
}
