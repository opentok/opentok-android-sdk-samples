package com.vonage.basic_video_chat_connectionservice

import android.view.View
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallHolder {
    private val _callFlow = MutableStateFlow<Call?>(null)
    val callFlow: Flow<Call?> = _callFlow.asStateFlow()

    private val _callStateFlow = MutableStateFlow(CallState.IDLE)
    val callStateFlow: Flow<CallState> = _callStateFlow.asStateFlow()

    private val subscriberMap = mutableMapOf<String, View>()

    private val _publisherViewFlow = MutableStateFlow<View?>(null)
    val publisherViewFlow: Flow<View?> = _publisherViewFlow.asStateFlow()

    private val _subscriberViewFlow = MutableStateFlow<List<View>>(emptyList())
    val subscriberViewFlow: Flow<List<View>> = _subscriberViewFlow.asStateFlow()

    fun setCall(call: Call?) {
        _callFlow.value = call
    }

    fun updateCallState(state: CallState) {
        _callStateFlow.value = state
    }

    fun setPublisherView(view: View?) {
        _publisherViewFlow.value = view
    }

    fun addSubscriberView(streamId: String, view: View) {
        subscriberMap[streamId] = view

        _subscriberViewFlow.value = subscriberMap.values.toList()
    }

    fun removeSubscriberView(streamId: String) {
        subscriberMap.remove(streamId)

        _subscriberViewFlow.value = subscriberMap.values.toList()
    }

    fun clear() {
        _callFlow.value = null
        _callStateFlow.value = CallState.IDLE
        subscriberMap.clear()
        _publisherViewFlow.value = null
        _subscriberViewFlow.value = emptyList()
    }
}