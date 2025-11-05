package com.tokbox.sample.basicvideochatconnectionservice.deviceselector

interface AudioDeviceSelectionListener {
    /**
     * Is called when the user selects an audio device.
     * @param device The selected audio device.
     */
    fun onAudioDeviceSelected(device: AudioDeviceSelector.AudioDevice)
}