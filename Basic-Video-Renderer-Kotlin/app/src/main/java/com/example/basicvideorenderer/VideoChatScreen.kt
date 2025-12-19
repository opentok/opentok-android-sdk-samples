package com.example.basicvideorenderer

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoChatScreen(
    publisherView: View?,
    subscriberView: View?
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Subscriber full-screen
        subscriberView?.let { view ->
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .fillMaxSize()
            )
        }?: run {
            Text(
                "Connect another subscriber to this session to see his/hers video",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical =
                        150.dp)
            )
        }


        publisherView?.let { view ->
            // Publisher picture-in-picture
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .size(width = 300.dp, height = 600.dp)
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(androidx.compose.ui.graphics.Color.LightGray)
            )
        }
    }
}