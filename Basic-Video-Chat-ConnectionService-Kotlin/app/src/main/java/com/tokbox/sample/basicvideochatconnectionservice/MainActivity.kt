package com.tokbox.sample.basicvideochatconnectionservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.tokbox.sample.basicvideochatconnectionservice.deviceselector.AudioDeviceSelector
import com.tokbox.sample.basicvideochatconnectionservice.deviceselector.AudioDeviceSelectorDialog
import com.tokbox.sample.basicvideochatconnectionservice.home.HomeView
import com.tokbox.sample.basicvideochatconnectionservice.home.HomeViewModel
import com.tokbox.sample.basicvideochatconnectionservice.room.RoomView
import com.tokbox.sample.basicvideochatconnectionservice.room.RoomViewModel
import com.tokbox.sample.basicvideochatconnectionservice.ui.theme.BasicVideoChatConnectionServiceKotlinTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val roomViewModel: RoomViewModel by viewModels()

    @Inject
    lateinit var powerManager: PowerManager
    @Inject
    lateinit var vonageManager: VonageManager
    @Inject
    lateinit var callHolder: CallHolder
    @Inject
    lateinit var audioDeviceSelector: AudioDeviceSelector

    val isBatteryOptimizationIgnored: Boolean
        get() = powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName)

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(RequestMultiplePermissions()) { result ->
            for ((permission, isGranted) in result) {
                Log.d("Permission", "$permission -> ${if (isGranted) "GRANTED" else "DENIED"}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        enableEdgeToEdge()
        setContent {
            val call by callHolder.callFlow.collectAsState(initial = null)
            var showAudioDeviceSelector by remember { mutableStateOf(false) }
            val error by vonageManager.errorFlow.collectAsState()

            BasicVideoChatConnectionServiceKotlinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (call != null) {
                        RoomView(
                            roomViewModel = roomViewModel,
                            onShowAudioDevicesClick = {
                                showAudioDeviceSelector = true
                        })
                    } else {
                        HomeView(
                            homeViewModel = homeViewModel,
                            modifier = Modifier.padding(innerPadding))
                    }

                    if (showAudioDeviceSelector) {
                        AudioDeviceSelectorDialog(
                            audioDeviceSelector = audioDeviceSelector,
                            onDismissRequest = {
                                showAudioDeviceSelector = false
                            }
                        )
                    }

                    error?.let { opentokError ->
                        OpenTokErrorDialog(
                            error = opentokError,
                            onDismiss = { vonageManager.clearError() },
                            onEndCall = {
                                vonageManager.clearError()
                                vonageManager.endCall()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.MANAGE_OWN_CALLS
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.MANAGE_OWN_CALLS
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.MANAGE_OWN_CALLS
            )
            else -> arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.BLUETOOTH,
            )
        }

        val permissionsNeeded = perms.any { permission ->
            ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded) {
            permissionLauncher.launch(perms)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public override fun onResume() {
        super.onResume()
        vonageManager.onResume()
    }

    public override fun onPause() {
        super.onPause()
        vonageManager.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        vonageManager.endSession()
    }
}