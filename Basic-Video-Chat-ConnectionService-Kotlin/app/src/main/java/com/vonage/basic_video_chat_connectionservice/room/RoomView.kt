package com.vonage.basic_video_chat_connectionservice.room

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoomView(roomViewModel: RoomViewModel, onShowAudioDevicesClick: () -> Unit) {
    val callStatus by roomViewModel.callStatusName.collectAsState()
    val callName by roomViewModel.callName.collectAsState()
    val isOnHold by roomViewModel.isOnHold.collectAsState()
    val publisherView by roomViewModel.publisherViewFlow.collectAsState()
    val subscriberViews by roomViewModel.subscriberViewsFlow.collectAsState()

    RoomView(
        callStatus = callStatus,
        participantName = callName,
        isPublisherVisible = true,
        isOnHoldVisible = isOnHold,
        publisherView = publisherView,
        subscriberViews = subscriberViews,
        onShowAudioDevicesClick = onShowAudioDevicesClick,
        onHangUpClick = roomViewModel::onEndCall,
        onUnHoldClick = roomViewModel::onUnhold
    )
}

@Composable
fun RoomView(
    callStatus: String,
    participantName: String,
    isPublisherVisible: Boolean,
    isOnHoldVisible: Boolean,
    publisherView: View?? = null,
    subscriberViews: List<View> = emptyList(),
    onShowAudioDevicesClick: () -> Unit,
    onHangUpClick: () -> Unit,
    onUnHoldClick: () -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x80000000))
                .padding(top = statusBarHeight + 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (callStatus.isNotEmpty()) {
                    Text(
                        text = callStatus,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (participantName.isNotEmpty()) {
                    Text(
                        text = participantName,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            Button(
                onClick = onShowAudioDevicesClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
            ) {
                Text("Audio")
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            SubscriberGrid(subscriberViews)

            if (isPublisherVisible && publisherView != null) {
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 120.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .background(Color(0xFFCCCCCC))
                ) {
                    PublisherView(
                        androidView = publisherView,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x80000000))
                .padding(
                    vertical = 8.dp,
                    horizontal = 16.dp
                ).padding(
                    bottom = navBarHeight + 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onHangUpClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("End call")
            }

            if (isOnHoldVisible) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onUnHoldClick) {
                    Text("Unhold call")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun RoomViewPreview() {
    RoomView(
        callStatus = "In call",
        participantName = "John Doe",
        isPublisherVisible = true,
        isOnHoldVisible = false,
        publisherView = null,
        subscriberViews = emptyList(),
        onShowAudioDevicesClick = {},
        onHangUpClick = {},
        onUnHoldClick = {}
    )
}

