package com.vonage.basic_video_chat_connectionservice.usecases

import com.vonage.basic_video_chat_connectionservice.CallException
import com.vonage.basic_video_chat_connectionservice.connectionservice.PhoneAccountManager
import javax.inject.Inject

class StartOutgoingCallUseCase @Inject constructor(
private val phoneAccountManager: PhoneAccountManager
) {

    operator fun invoke() {
        val callerName = "Simulated Caller"
        val callerId = "+4401539702257"

        if (phoneAccountManager.canPlaceOutgoingCall()) {
            phoneAccountManager.startOutgoingVideoCall(callerName, callerId)
        } else {
            throw CallException("Can't launch outgoing call", 1001)
        }
    }
}