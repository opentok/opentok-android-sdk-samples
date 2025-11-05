package com.tokbox.sample.basicvideochatconnectionservice.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tokbox.sample.basicvideochatconnectionservice.CallHolder
import com.tokbox.sample.basicvideochatconnectionservice.CallState
import com.tokbox.sample.basicvideochatconnectionservice.usecases.EndCallUseCase
import com.tokbox.sample.basicvideochatconnectionservice.usecases.UnholdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
private val callHolder: CallHolder,
private val endCallUseCase: EndCallUseCase,
private val unholdUseCase: UnholdUseCase
): ViewModel() {

    val publisherViewFlow = callHolder.publisherViewFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val subscriberViewsFlow = callHolder.subscriberViewFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val callStatusName = callHolder.callStateFlow
        .map { state ->
            when (state) {
                CallState.IDLE -> "Idle"
                CallState.CONNECTING -> "Connecting"
                CallState.CONNECTED -> "Connected"
                CallState.DIALING -> "Dialing"
                CallState.ANSWERING -> "Answering"
                CallState.HOLDING -> "Holding"
                CallState.DISCONNECTED -> "Disconnected"
            }
    }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "Idle"
    )

    val callName = callHolder.callFlow
        .map { call ->
            call?.name ?: "No Call"
    }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "No Call"
    )

    val isOnHold = callHolder.callStateFlow
        .map { it == CallState.HOLDING }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    fun onEndCall() {
        endCallUseCase()
    }

    fun onUnhold() {
        unholdUseCase()
    }
}