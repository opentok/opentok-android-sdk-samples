package com.tokbox.basic_video_chat_connectionservice.room

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PublisherView(
    androidView: android.view.View?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            android.widget.FrameLayout(context)
        },
        update = { container ->
            container.removeAllViews()

            androidView?.let { view ->
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                container.addView(view)
            }
        },
        modifier = modifier
    )
}