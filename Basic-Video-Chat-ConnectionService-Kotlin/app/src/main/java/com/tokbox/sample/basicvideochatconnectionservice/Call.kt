package com.tokbox.sample.basicvideochatconnectionservice

data class Call(
    val callID: Int,
    val name: String,
    val state: CallState
)

enum class CallState {
    IDLE,
    CONNECTING,
    DIALING,
    ANSWERING,
    CONNECTED,
    HOLDING,
    DISCONNECTED
}
