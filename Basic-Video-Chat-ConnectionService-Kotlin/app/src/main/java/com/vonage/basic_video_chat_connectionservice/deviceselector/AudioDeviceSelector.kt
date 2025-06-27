package com.vonage.basic_video_chat_connectionservice.deviceselector

import android.os.Build
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class AudioDeviceSelector {
    private val TAG: String = AudioDeviceSelector::class.java.simpleName

    class AudioDevice {
        val name: String
        val type: Int
        val endpoint: CallEndpoint?

        internal constructor(name: String, type: Int) {
            this.name = name
            this.type = type
            this.endpoint = null
        }

        internal constructor(name: String, type: Int, endpoint: CallEndpoint?) {
            this.name = name
            this.type = type
            this.endpoint = endpoint
        }
    }

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<AudioDevice?>(null)
    val activeDevice: StateFlow<AudioDevice?> = _activeDevice.asStateFlow()

    var listener: AudioDeviceSelectionListener? = null

    fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        val devices: MutableList<AudioDevice> = ArrayList()

        for (endpoint in endpoints) {
            val name: String
            val type: Int

            when (endpoint.endpointType) {
                CallEndpoint.TYPE_BLUETOOTH -> {
                    name = "Bluetooth"
                    type = CallAudioState.ROUTE_BLUETOOTH
                }

                CallEndpoint.TYPE_WIRED_HEADSET -> {
                    name = "Wired Headset"
                    type = CallAudioState.ROUTE_WIRED_HEADSET
                }

                CallEndpoint.TYPE_SPEAKER -> {
                    name = "Speaker"
                    type = CallAudioState.ROUTE_SPEAKER
                }

                CallEndpoint.TYPE_EARPIECE -> {
                    name = "Earpiece"
                    type = CallAudioState.ROUTE_EARPIECE
                }

                else -> {
                    name = "Unknown"
                    type = 0
                }
            }

            devices.add(AudioDevice(name, type, endpoint))
        }

        _availableDevices.value = devices
    }

    fun onCallEndpointChanged(endpoint: CallEndpoint) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        val devices = availableDevices.value ?: return

        for (device in devices) {
            if (device.endpoint != null &&
                device.endpoint.endpointType == endpoint.endpointType
            ) {
                _activeDevice.value = device
                break
            }
        }
    }

    fun onCallAudioStateChanged(audioState: CallAudioState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // In Android 14+, use CallEndpoint for audio routing
            return
        }

        val devices: MutableList<AudioDevice> = ArrayList()
        val supportedRoutes = audioState.supportedRouteMask
        val currentRoute = audioState.route

        if ((supportedRoutes and CallAudioState.ROUTE_EARPIECE) != 0) {
            val device = AudioDevice("Earpiece", CallAudioState.ROUTE_EARPIECE)
            devices.add(device)
            if (currentRoute == CallAudioState.ROUTE_EARPIECE) {
                _activeDevice.value = device
            }
        }

        if ((supportedRoutes and CallAudioState.ROUTE_BLUETOOTH) != 0) {
            val device = AudioDevice("Bluetooth", CallAudioState.ROUTE_BLUETOOTH)
            devices.add(device)
            if (currentRoute == CallAudioState.ROUTE_BLUETOOTH) {
                _activeDevice.value = device
            }
        }

        if ((supportedRoutes and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
            val device = AudioDevice("Wired Headset", CallAudioState.ROUTE_WIRED_HEADSET)
            devices.add(device)
            if (currentRoute == CallAudioState.ROUTE_WIRED_HEADSET) {
                _activeDevice.value = device
            }
        }

        if ((supportedRoutes and CallAudioState.ROUTE_SPEAKER) != 0) {
            val device = AudioDevice("Speaker", CallAudioState.ROUTE_SPEAKER)
            devices.add(device)
            if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
                _activeDevice.value = device
            }
        }

        _availableDevices.value = devices
    }

    fun selectDevice(device: AudioDevice) {
        listener?.onAudioDeviceSelected(device)
    }
}