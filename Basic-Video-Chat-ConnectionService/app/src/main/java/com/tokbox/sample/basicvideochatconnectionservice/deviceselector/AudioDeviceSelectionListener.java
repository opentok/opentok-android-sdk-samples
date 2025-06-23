package com.tokbox.sample.basicvideochatconnectionservice.deviceselector;

public interface AudioDeviceSelectionListener {
    /**
     * Is called when the user selects an audio device.
     * @param device The selected audio device.
     */
    void onAudioDeviceSelected(AudioDeviceSelector.AudioDevice device);
}