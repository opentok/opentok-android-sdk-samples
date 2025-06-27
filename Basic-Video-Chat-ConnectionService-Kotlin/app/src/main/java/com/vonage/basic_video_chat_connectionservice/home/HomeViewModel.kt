package com.vonage.basic_video_chat_connectionservice.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vonage.basic_video_chat_connectionservice.usecases.StartIncomingCallUseCase
import com.vonage.basic_video_chat_connectionservice.usecases.StartOutgoingCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startIncomingCallUseCase: StartIncomingCallUseCase,
    private val startOutgoingCallUseCase: StartOutgoingCallUseCase
): ViewModel() {

    fun startOutgoingCall() {
        viewModelScope.launch {
            startOutgoingCallUseCase()
        }
    }

    fun startIncomingCall() {
        viewModelScope.launch {
            startIncomingCallUseCase()
        }
    }
}