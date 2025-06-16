package com.tokbox.sample.basicvideochat_connectionservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tokbox.sample.basicvideochat_connectionservice.connectionservice.VonageConnection;
import com.tokbox.sample.basicvideochat_connectionservice.connectionservice.VonageConnectionHolder;

public class CallActionReceiver extends BroadcastReceiver {

    private static final String TAG = "CallActionReceiver";

    public static final String ACTION_ANSWER_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_ANSWER_CALL";
    public static final String ACTION_REJECT_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_REJECT_CALL";
    public static final String ACTION_END_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_END_CALL";
    public static final String ACTION_ANSWERED_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_ANSWERED_CALL";
    public static final String ACTION_INCOMING_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_INCOMING_CALL";
    public static final String ACTION_NOTIFY_INCOMING_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_NOTIFY_INCOMING_CALL";
    public static final String ACTION_REJECTED_CALL = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_REJECTED_CALL";
    public static final String ACTION_CALL_ENDED = "com.tokbox.sample.basicvideochat_connectionservice.ACTION_CALL_ENDED";
    public static final int ACTION_ANSWER_CALL_ID = 2;
    public static final int ACTION_REJECT_CALL_ID = 3;
    public static final int ACTION_END_CALL_ID = 4;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_ANSWER_CALL:
                    Log.d(TAG, "Action: Answer call");
                    answerCall();
                    break;
                case ACTION_REJECT_CALL:
                    Log.d(TAG, "Action: Reject call");
                    rejectCall();
                    break;
                case ACTION_END_CALL:
                    Log.d(TAG, "Action: End call");
                    endCall();
                    break;
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        }
    }

    private void answerCall() {
        VonageConnection connection = VonageConnectionHolder.getInstance().getConnection();

        if (connection != null) {
            Log.d(TAG, "Call answered");
            connection.onAnswer();
        } else {
            Log.w(TAG, "No active connection to answer the call");
        }
    }

    private void rejectCall() {
        VonageConnection connection = VonageConnectionHolder.getInstance().getConnection();

        if (connection != null) {
            Log.d(TAG, "Call rejected");
            connection.onReject();
        } else {
            Log.w(TAG, "No active connection to reject the call");
        }
    }

    private void endCall() {
        VonageConnection connection = VonageConnectionHolder.getInstance().getConnection();

        if (connection != null) {
            Log.d(TAG, "Call ended");
            connection.onDisconnect();
        } else {
            Log.w(TAG, "No active connection to end the call");
        }
    }
}