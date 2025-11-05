package com.tokbox.basic_video_chat_connectionservice.usecases

import com.tokbox.basic_video_chat_connectionservice.connectionservice.VonageConnectionHolder
import javax.inject.Inject

class EndCallUseCase @Inject constructor() {

    operator fun invoke() {
        VonageConnectionHolder.connection?.onDisconnect()
    }
}