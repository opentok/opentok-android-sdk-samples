package com.vonage.basic_video_chat_connectionservice.usecases

import com.vonage.basic_video_chat_connectionservice.connectionservice.VonageConnectionHolder
import javax.inject.Inject

class UnholdUseCase @Inject constructor() {

    operator fun invoke() {
        VonageConnectionHolder.connection?.onUnhold()
    }
}