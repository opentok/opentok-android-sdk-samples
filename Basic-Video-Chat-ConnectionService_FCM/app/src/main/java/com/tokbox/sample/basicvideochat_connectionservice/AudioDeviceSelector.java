package com.tokbox.sample.basicvideochat_connectionservice;

import android.os.Build;
import android.telecom.CallAudioState;
import android.telecom.CallEndpoint;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class AudioDeviceSelector {
    private static final String TAG = AudioDeviceSelector.class.getSimpleName();

    public static class AudioDevice {
        private final String name;
        private final int type;
        private final CallEndpoint endpoint;

        AudioDevice(String name, int type) {
            this.name = name;
            this.type = type;
            this.endpoint = null;
        }

        AudioDevice(String name, int type, CallEndpoint endpoint) {
            this.name = name;
            this.type = type;
            this.endpoint = endpoint;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public CallEndpoint getEndpoint() {
            return endpoint;
        }
    }

    private final MutableLiveData<List<AudioDevice>> availableDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<AudioDevice> activeDevice = new MutableLiveData<>();
    private AudioDeviceSelectionListener listener;
    private static AudioDeviceSelector instance;

    private AudioDeviceSelector() {}

    public static synchronized AudioDeviceSelector getInstance() {
        if (instance == null) {
            instance = new AudioDeviceSelector();
        }
        return instance;
    }

    public void setAudioDeviceSelectionListener(AudioDeviceSelectionListener listener) {
        this.listener = listener;
    }

    public LiveData<List<AudioDevice>> getAvailableDevices() {
        return availableDevices;
    }

    public LiveData<AudioDevice> getActiveDevice() {
        return activeDevice;
    }

    public void onAvailableCallEndpointsChanged(List<CallEndpoint> endpoints) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return;
        }

        List<AudioDevice> devices = new ArrayList<>();

        for (CallEndpoint endpoint : endpoints) {
            String name;
            int type;

            switch (endpoint.getEndpointType()) {
                case CallEndpoint.TYPE_BLUETOOTH:
                    name = "Bluetooth";
                    type = CallAudioState.ROUTE_BLUETOOTH;
                    break;
                case CallEndpoint.TYPE_WIRED_HEADSET:
                    name = "Wired Headset";
                    type = CallAudioState.ROUTE_WIRED_HEADSET;
                    break;
                case CallEndpoint.TYPE_SPEAKER:
                    name = "Speaker";
                    type = CallAudioState.ROUTE_SPEAKER;
                    break;
                case CallEndpoint.TYPE_EARPIECE:
                    name = "Earpiece";
                    type = CallAudioState.ROUTE_EARPIECE;
                    break;
                default:
                    name = "Unknown";
                    type = 0;
                    break;
            }

            devices.add(new AudioDevice(name, type, endpoint));
        }

        availableDevices.setValue(devices);
    }

    public void onCallEndpointChanged(CallEndpoint endpoint) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return;
        }

        List<AudioDevice> devices = availableDevices.getValue();
        if (devices == null) return;

        for (AudioDevice device : devices) {
            if (device.getEndpoint() != null &&
                    device.getEndpoint().getEndpointType() == endpoint.getEndpointType()) {
                activeDevice.postValue(device);
                break;
            }
        }
    }

    public void onCallAudioStateChanged(CallAudioState audioState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // In Android 14+, use CallEndpoint for audio routing
            return;
        }

        List<AudioDevice> devices = new ArrayList<>();
        int supportedRoutes = audioState.getSupportedRouteMask();
        int currentRoute = audioState.getRoute();

        if ((supportedRoutes & CallAudioState.ROUTE_EARPIECE) != 0) {
            AudioDevice device = new AudioDevice("Earpiece", CallAudioState.ROUTE_EARPIECE);
            devices.add(device);
            if (currentRoute == CallAudioState.ROUTE_EARPIECE) {
                activeDevice.postValue(device);
            }
        }

        if ((supportedRoutes & CallAudioState.ROUTE_BLUETOOTH) != 0) {
            AudioDevice device = new AudioDevice("Bluetooth", CallAudioState.ROUTE_BLUETOOTH);
            devices.add(device);
            if (currentRoute == CallAudioState.ROUTE_BLUETOOTH) {
                activeDevice.postValue(device);
            }
        }

        if ((supportedRoutes & CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
            AudioDevice device = new AudioDevice("Wired Headset", CallAudioState.ROUTE_WIRED_HEADSET);
            devices.add(device);
            if (currentRoute == CallAudioState.ROUTE_WIRED_HEADSET) {
                activeDevice.postValue(device);
            }
        }

        if ((supportedRoutes & CallAudioState.ROUTE_SPEAKER) != 0) {
            AudioDevice device = new AudioDevice("Speaker", CallAudioState.ROUTE_SPEAKER);
            devices.add(device);
            if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
                activeDevice.postValue(device);
            }
        }

        availableDevices.postValue(devices);
    }

    public void selectDevice(AudioDevice device) {
        if (listener == null) {
            Log.e(TAG, "No listener set for audio device selection");
            return;
        }

        listener.onAudioDeviceSelected(device);
    }
}