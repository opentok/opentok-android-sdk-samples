package com.vonage.basic_video_chat_connectionservice.deviceselector

interface AudioDeviceSelectionListener {
    /**
     * Is called when the user selects an audio device.
     * @param device The selected audio device.
     */
    fun onAudioDeviceSelected(device: AudioDeviceSelector.AudioDevice)
}