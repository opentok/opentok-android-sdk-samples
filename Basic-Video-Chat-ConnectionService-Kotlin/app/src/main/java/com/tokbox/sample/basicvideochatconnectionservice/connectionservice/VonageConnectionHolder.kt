package com.tokbox.sample.basicvideochatconnectionservice.connectionservice

import android.annotation.SuppressLint
import com.tokbox.basic_video_chat_connectionservice.connectionservice.VonageConnection

object VonageConnectionHolder {
    @SuppressLint("StaticFieldLeak")
    var connection: VonageConnection? = null
}