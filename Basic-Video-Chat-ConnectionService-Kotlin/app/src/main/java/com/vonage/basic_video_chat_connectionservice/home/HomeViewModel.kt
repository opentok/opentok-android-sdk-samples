package com.vonage.basic_video_chat_connectionservice.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vonage.basic_video_chat_connectionservice.usecases.StartIncomingCallUseCase
import com.vonage.basic_video_chat_connectionservice.usecases.StartOutgoingCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startIncomingCallUseCase: StartIncomingCallUseCase,
    private val startOutgoingCallUseCase: StartOutgoingCallUseCase
): ViewModel() {

    private val _errorFlow = MutableStateFlow<Exception?>(null)
    val errorFlow = _errorFlow.asStateFlow()

    fun startOutgoingCall() {
        viewModelScope.launch {
            try {
                startOutgoingCallUseCase()
            } catch (exception: Exception) {
                _errorFlow.value = exception
            }
        }
    }

    fun startIncomingCall() {
        viewModelScope.launch {
            try {
                startIncomingCallUseCase()
            } catch (exception: Exception) {
                _errorFlow.value = exception
            }
        }
    }

    fun clearError() {
        _errorFlow.value = null
    }
}