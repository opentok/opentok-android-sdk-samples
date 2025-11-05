package com.tokbox.basic_video_chat_connectionservice.di

import android.content.Context
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import android.telecom.TelecomManager
import com.opentok.android.AudioDeviceManager
import com.tokbox.basic_video_chat_connectionservice.CallHolder
import com.tokbox.basic_video_chat_connectionservice.VonageManager
import com.tokbox.basic_video_chat_connectionservice.connectionservice.PhoneAccountManager
import com.tokbox.basic_video_chat_connectionservice.deviceselector.AudioDeviceSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.tokbox.basic_video_chat_connectionservice.NotificationChannelManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideVonageManager(
        @ApplicationContext context: Context,
        audioDeviceManager: AudioDeviceManager,
        callHolder: CallHolder
    ): VonageManager {
        val manager = VonageManager(context, audioDeviceManager, callHolder)
        manager.setAudioFocusManager()
        return manager
    }

    @Provides
    @Singleton
    fun provideAudioDeviceManager(
        @ApplicationContext context: Context
    ): AudioDeviceManager {
        return AudioDeviceManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioDeviceSelector(
    ): AudioDeviceSelector {
        return AudioDeviceSelector()
    }

    @Provides
    @Singleton
    fun providePhoneAccountManager(
        @ApplicationContext context: Context,
        telecomManager: TelecomManager
    ): PhoneAccountManager {
        return PhoneAccountManager(context, telecomManager)
    }

    @Provides
    @Singleton
    fun provideTelecomManager(
        @ApplicationContext context: Context
    ): TelecomManager {
        return context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }

    @Provides
    @Singleton
    fun providePowerManager(
        @ApplicationContext context: Context
    ): PowerManager {
        return context.getSystemService(POWER_SERVICE) as PowerManager
    }

    @Provides
    @Singleton
    fun provideNotificationChannelManager(
        @ApplicationContext context: Context
    ): NotificationChannelManager {
        return NotificationChannelManager(context)
    }

    @Provides
    @Singleton
    fun provideCallHolder(
    ): CallHolder {
        return CallHolder()
    }
}