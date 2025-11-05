package com.tokbox.sample.basicvideochatconnectionservice

import android.app.Application
import android.os.Build
import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.PhoneAccountManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {
    @Inject lateinit var phoneAccountManager: PhoneAccountManager
    @Inject lateinit var notificationChannelManager: NotificationChannelManager

    override fun onCreate() {
        super.onCreate()

        phoneAccountManager.registerPhoneAccount()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannelManager.setup()
        }
    }
}