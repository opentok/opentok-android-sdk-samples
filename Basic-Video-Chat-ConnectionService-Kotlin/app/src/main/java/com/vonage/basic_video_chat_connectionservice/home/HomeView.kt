package com.vonage.basic_video_chat_connectionservice.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeView(homeViewModel: HomeViewModel, modifier: Modifier) {
    HomeView(
        onOutgoingCall = homeViewModel::startOutgoingCall,
        onIncomingCall = homeViewModel::startIncomingCall,
        modifier = modifier)
}

@Composable
fun HomeView(
    onOutgoingCall: () -> Unit = {},
    onIncomingCall: () -> Unit = {},
    modifier: Modifier
) {

    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Button(onClick = {
            onIncomingCall()
        }) {
            Text(text = "Incoming Call")
        }
        Button(onClick = {
            onOutgoingCall()
        }) {
            Text(text = "Outgoing Call")
        }
    }
}

@Preview
@Composable
fun HomeViewPreview() {
    HomeView(modifier = Modifier)
}