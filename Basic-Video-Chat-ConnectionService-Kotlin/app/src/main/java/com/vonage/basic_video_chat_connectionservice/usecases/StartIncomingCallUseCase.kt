package com.vonage.basic_video_chat_connectionservice.usecases

import com.vonage.basic_video_chat_connectionservice.CallException
import com.vonage.basic_video_chat_connectionservice.OpenTokConfig
import com.vonage.basic_video_chat_connectionservice.connectionservice.PhoneAccountManager
import javax.inject.Inject

class StartIncomingCallUseCase @Inject constructor(
private val phoneAccountManager: PhoneAccountManager
) {

    operator fun invoke() {
        val callerName = "Simulated Caller"
        val callerId = "+4401539702257"

        if (!OpenTokConfig.isValid) {
            throw CallException("Invalid credentials", 1001)
        }

        if (!phoneAccountManager.canPlaceIncomingCall()) {
            throw CallException("Can't launch incoming call", 1001)
        }

        phoneAccountManager.notifyIncomingVideoCall(callerName, callerId)
    }
}