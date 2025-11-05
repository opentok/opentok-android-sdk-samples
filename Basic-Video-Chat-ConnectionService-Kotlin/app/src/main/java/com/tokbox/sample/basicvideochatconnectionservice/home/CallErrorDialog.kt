package com.tokbox.sample.basicvideochatconnectionservice.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tokbox.sample.basicvideochatconnectionservice.CallException

@Composable
fun CallErrorDialog(
    error: Exception,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = {
            if (error is CallException) {
                Text("Error (${error.code}): ${error.message}")
            } else {
                Text(error.message ?: "Error desconocido")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        }
    )
}