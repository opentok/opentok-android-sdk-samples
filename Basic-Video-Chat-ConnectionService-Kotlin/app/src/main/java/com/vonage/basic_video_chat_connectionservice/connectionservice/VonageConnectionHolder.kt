package com.vonage.basic_video_chat_connectionservice.connectionservice

import android.annotation.SuppressLint

object VonageConnectionHolder {
    @SuppressLint("StaticFieldLeak")
    var connection: VonageConnection? = null
}