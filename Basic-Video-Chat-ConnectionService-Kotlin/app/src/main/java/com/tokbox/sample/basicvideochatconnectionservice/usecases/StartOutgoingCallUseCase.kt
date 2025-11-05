package com.tokbox.sample.basicvideochatconnectionservice.usecases

import com.tokbox.sample.basicvideochatconnectionservice.CallException
import com.tokbox.sample.basicvideochatconnectionservice.OpenTokConfig
import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.PhoneAccountManager
import javax.inject.Inject

class StartOutgoingCallUseCase @Inject constructor(
private val phoneAccountManager: PhoneAccountManager
) {

    operator fun invoke() {
        val callerName = "Simulated Caller"
        val callerId = "+4401539702257"

        if (!OpenTokConfig.isValid) {
            throw CallException("Invalid credentials", 1001)
        }

        if (!phoneAccountManager.canPlaceOutgoingCall()) {
            throw CallException("Can't launch outgoing call", 1001)
        }

        phoneAccountManager.startOutgoingVideoCall(callerName, callerId)
    }
}