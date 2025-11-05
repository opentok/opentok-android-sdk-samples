package com.tokbox.sample.basicvideochatconnectionservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.VonageConnectionHolder

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && intent.action != null) {
            val action = intent.action
            when (action) {
                ACTION_ANSWER_CALL -> {
                    Log.d(TAG, "Action: Answer call")
                    answerCall()
                }

                ACTION_REJECT_CALL -> {
                    Log.d(TAG, "Action: Reject call")
                    rejectCall()
                }

                ACTION_END_CALL -> {
                    Log.d(TAG, "Action: End call")
                    endCall()
                }

                else -> Log.w(
                    TAG,
                    "Unknown action: $action"
                )
            }
        }
    }

    private fun answerCall() {
        VonageConnectionHolder.connection?.onAnswer()
    }

    private fun rejectCall() {
        VonageConnectionHolder.connection?.onReject()
    }

    private fun endCall() {
        VonageConnectionHolder.connection?.onDisconnect()
    }

    companion object {
        private const val TAG = "CallActionReceiver"

        const val ACTION_ANSWER_CALL: String =
            "com.tokbox.sample.basicvideochatconnectionservice.ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL: String =
            "com.tokbox.sample.basicvideochatconnectionservice.ACTION_REJECT_CALL"
        const val ACTION_END_CALL: String =
            "com.tokbox.sample.basicvideochatconnectionservice.ACTION_END_CALL"

        const val ACTION_ANSWER_CALL_ID: Int = 2
        const val ACTION_REJECT_CALL_ID: Int = 3
        const val ACTION_END_CALL_ID: Int = 4
    }
}