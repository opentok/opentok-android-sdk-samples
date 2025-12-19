package com.tokbox.sample.basicvideochatconnectionservice.connectionservice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.app.ActivityCompat
import com.tokbox.sample.basicvideochatconnectionservice.R

class PhoneAccountManager(
    private val context: Context,
    private val telecomManager: TelecomManager
) {
    var handle: PhoneAccountHandle? = null

    fun registerPhoneAccount() {

        val componentName: ComponentName =
            ComponentName(context, VonageConnectionService::class.java)
        handle = PhoneAccountHandle(componentName, ACCOUNT_ID)
        val phoneAccount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PhoneAccount.builder(handle, "Vonage Video")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED or PhoneAccount.CAPABILITY_VIDEO_CALLING)
                .setHighlightColor(Color.BLUE)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                .addSupportedUriScheme(VONAGE_CALL_SCHEME)
                .build()
        } else {
            PhoneAccount.builder(handle, "Vonage Video")
                .setCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)
                .setHighlightColor(Color.BLUE)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                .addSupportedUriScheme(VONAGE_CALL_SCHEME)
                .build()
        }

        telecomManager.registerPhoneAccount(phoneAccount)
        Log.d("PhoneAccountManager", "PhoneAccount registered: " + phoneAccount.isEnabled)
    }

    fun startOutgoingVideoCall(callerName: String?, callerId: String?) {
        val extras = Bundle()
        extras.putString(CALLER_NAME, callerName)
        extras.putString(CALLER_ID, callerId)
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        extras.putInt(
            TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
            VideoProfile.STATE_BIDIRECTIONAL
        )

        val calleeUri = Uri.Builder()
            .scheme(VONAGE_CALL_SCHEME)
            .authority(callerId)
            .appendQueryParameter("callerName", callerName)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        telecomManager.placeCall(calleeUri, extras)
    }

    fun notifyIncomingVideoCall(callerName: String?, callerId: String?) {
        val extras = Bundle()
        extras.putString(CALLER_NAME, callerName)
        extras.putString(CALLER_ID, callerId)
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)

        val calleeUri = Uri.Builder()
            .scheme(VONAGE_CALL_SCHEME)
            .authority(callerId)
            .appendQueryParameter("callerName", callerName)
            .build()

        extras.putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, calleeUri.toString())

        if (handle != null) {
            telecomManager.addNewIncomingCall(handle, extras)
            Log.d("PhoneAccountManager", "Incoming video call notified.")
        } else {
            Log.e(
                "PhoneAccountManager",
                "TelecomManager or PhoneAccountHandle is null. Cannot notify incoming call."
            )
        }
    }

    fun canPlaceIncomingCall(): Boolean {
        if (handle == null) {
            Log.e("PhoneAccountManager", "TelecomManager or PhoneAccountHandle is not initialized.")
            return false
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telecomManager.isIncomingCallPermitted(handle)
        } else {
            true
        }
    }

    fun canPlaceOutgoingCall(): Boolean {
        if (handle == null) {
            Log.e("PhoneAccountManager", "TelecomManager or PhoneAccountHandle is not initialized.")
            return false
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telecomManager.isOutgoingCallPermitted(handle)
        } else {
            true
        }
    }

    companion object {
        const val ACCOUNT_ID: String = "vonage_video_call"
        private const val VONAGE_CALL_SCHEME = "vonagecall"
        var CALLER_NAME: String = "CALLER_NAME"
        var CALLER_ID: String = "CALLER_ID"
    }
}
