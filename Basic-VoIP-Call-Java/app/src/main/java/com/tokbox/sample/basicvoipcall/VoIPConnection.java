package com.tokbox.sample.basicvoipcall;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.telecom.CallAudioState;
import android.telecom.Connection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class VoIPConnection extends Connection {

    public VoIPConnection() {
        super();
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        super.onCallAudioStateChanged(state);
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
    }

    @Override
    public void onHold() {
        super.onHold();
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
    }

    @Override
    public void onReject() {
        super.onReject();
    }

    @Override
    public void onShowIncomingCallUi() {
        super.onShowIncomingCallUi();
    }
}

