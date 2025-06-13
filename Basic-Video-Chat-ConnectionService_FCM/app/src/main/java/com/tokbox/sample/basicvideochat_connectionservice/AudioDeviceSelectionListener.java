package com.tokbox.sample.basicvideochat_connectionservice;

public interface AudioDeviceSelectionListener {
    /**
     * Is called when the user selects an audio device.
     * @param device The selected audio device.
     */
    void onAudioDeviceSelected(AudioDeviceSelector.AudioDevice device);
}