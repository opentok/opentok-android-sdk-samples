package com.tokbox.sample.basicvideochatconnectionservice.deviceselector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AudioDeviceSelectorDialog(
    audioDeviceSelector: AudioDeviceSelector,
    onDismissRequest: () -> Unit
) {
    val availableDevices by audioDeviceSelector.availableDevices.collectAsState()
    val activeDevice by audioDeviceSelector.activeDevice.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Audio devices") },
        text = {
            LazyColumn {
                items(availableDevices) { device ->
                    val isSelected = device == activeDevice

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                audioDeviceSelector.selectDevice(device)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                audioDeviceSelector.selectDevice(device)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = device.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}