package com.tokbox.sample.basicvoipcall;

import android.content.Context;
import android.os.Build;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class VoIPConnection extends Connection {

    private static final String TAG = "VoIPConnection";
    private final Context mContext;

    public VoIPConnection(Context Context) {
        super();
        Log.i(TAG, "VoIPConnection() ctor");
        this.mContext = Context;
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        Log.i(TAG, "onCallAudioStateChanged(state) || state = " + state);
        super.onCallAudioStateChanged(state);
    }

    @Override
    public void onStateChanged(int state) {
        Log.i(TAG, "onStateChanged()");
        super.onStateChanged(state);
    }

    @Override
    public void onDisconnect() {
        Log.i(TAG, "onDisconnect()");
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.BUSY));
        destroy();
    }

    @Override
    public void onHold() {
        Log.i(TAG, "onHold()");
        super.onHold();
    }

    @Override
    public void onAnswer() {
        Log.i(TAG, "onAnswer()");
        super.onAnswer();
    }

    @Override
    public void onReject() {
        Log.i(TAG, "onReject()");
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        destroy();
    }

    @Override
    public void onShowIncomingCallUi() {
        Log.i(TAG, "onShowIncomingCallUi()");
        super.onShowIncomingCallUi();
    }
}

