package com.tokbox.sample.basicvideochatconnectionservice.usecases

import com.tokbox.sample.basicvideochatconnectionservice.connectionservice.VonageConnectionHolder
import javax.inject.Inject

class EndCallUseCase @Inject constructor() {

    operator fun invoke() {
        VonageConnectionHolder.connection?.onDisconnect()
    }
}