package com.tokbox.basic_video_chat_connectionservice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opentok.android.OpentokError

@Composable
fun OpenTokErrorDialog(
    error: OpentokError,
    onDismiss: () -> Unit,
    onEndCall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection error") },
        text = {
            Column {
                Text("A communication error happened:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.message ?: "Unknown error",
                    fontWeight = FontWeight.Bold
                )
                Text("Código: ${error.errorCode}")
            }
        },
        confirmButton = {
            TextButton(onClick = onEndCall) {
                Text("Finish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.error
    )
}