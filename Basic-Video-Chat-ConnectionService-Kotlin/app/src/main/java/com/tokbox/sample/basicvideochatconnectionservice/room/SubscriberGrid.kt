package com.tokbox.sample.basicvideochatconnectionservice.room

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SubscriberGrid(
    subscriberViews: List<View>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val columns = when {
            subscriberViews.isEmpty() -> 1
            subscriberViews.size == 1 -> 1
            subscriberViews.size <= 4 -> 2
            subscriberViews.size <= 9 -> 3
            else -> 4
        }

        val cellWidth = screenWidth / columns
        val cellHeight = cellWidth * 4/3

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.padding(4.dp)
        ) {
            items(subscriberViews.size) { index ->
                val subscriberView = subscriberViews[index]
                Box(
                    modifier = Modifier
                        .width(cellWidth - 8.dp)
                        .height(cellHeight - 8.dp)
                        .padding(4.dp)
                        .background(Color(0xFFCCCCCC))
                ) {
                    PublisherView(
                        androidView = subscriberView,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}