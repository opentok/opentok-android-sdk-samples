package com.vonage.basic_video_chat_connectionservice.usecases

import com.vonage.basic_video_chat_connectionservice.CallException
import com.vonage.basic_video_chat_connectionservice.connectionservice.PhoneAccountManager
import javax.inject.Inject

class StartIncomingCallUseCase @Inject constructor(
private val phoneAccountManager: PhoneAccountManager
) {

    operator fun invoke() {
        val callerName = "Simulated Caller"
        val callerId = "+4401539702257"

        if (phoneAccountManager.canPlaceIncomingCall()) {
            phoneAccountManager.notifyIncomingVideoCall(callerName, callerId)
        } else {
            throw CallException("Can't launch incoming call", 1001)
        }
    }
}