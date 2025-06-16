package com.tokbox.sample.basicvideochat_connectionservice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AudioDeviceDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AudioDeviceSelector deviceSelector = AudioDeviceSelector.getInstance();

        List<AudioDeviceSelector.AudioDevice> devices = deviceSelector.getAvailableDevices().getValue();
        if (devices == null) {
            devices = new ArrayList<>();
        }

        String[] deviceNames = new String[devices.size()];
        int checkedItem = -1;
        AudioDeviceSelector.AudioDevice activeDevice = deviceSelector.getActiveDevice().getValue();

        for (int i = 0; i < devices.size(); i++) {
            deviceNames[i] = devices.get(i).getName();
            if (activeDevice != null && devices.get(i).getType() == activeDevice.getType()) {
                checkedItem = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        List<AudioDeviceSelector.AudioDevice> finalDevices = devices;
        builder.setTitle("Select audio device")
                .setSingleChoiceItems(deviceNames, checkedItem, (dialog, which) -> {
                    AudioDeviceSelector.AudioDevice selectedDevice = finalDevices.get(which);
                    deviceSelector.selectDevice(selectedDevice);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null);

        return builder.create();
    }
}