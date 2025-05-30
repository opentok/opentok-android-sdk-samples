package com.tokbox.sample.basicvideochat_connectionservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

import com.opentok.android.OtLog;

import java.util.List;

class BluetoothManager {
    // Bluetooth connection state.
    private enum State {
        // No bluetooth audio SCO connection
        SCO_DISCONNECTED,
        // Bluetooth audio SCO connection with remote device is initiated.
        SCO_CONNECTING,
        // Connection attempt scheduled
        SCO_CONNECT_ATTEMPT,
        // Bluetooth audio SCO connection with remote device is established.
        SCO_CONNECTED
    }

    final OtLog.LogToken log = new OtLog.LogToken(this);

    private State bluetoothConnectionState;
    private boolean isBluetoothBroadcastReceiverRegistered;
    int scoConnectionAttempts = 0;
    public static final int MAX_SCO_CONNECTION_ATTEMPTS = 6;
    public static final int SCO_CONNECTION_ATTEMPT_DELAY = 25;
    private BluetoothAdapter bluetoothAdapter;
    BluetoothProfile bluetoothProfile;
    private final AudioManager audioManager;
    Context audioDeviceContext;
    AdvancedAudioDevice audioDevice;
    BluetoothDevice bluetoothDevice;

    /**
     * Construction.
     */
    static BluetoothManager create(Context context, AdvancedAudioDevice audioDevice) {
        return new BluetoothManager(context, audioDevice);
    }

    protected BluetoothManager(Context context, AdvancedAudioDevice defaultAudioDevice) {
        log.d("BluetoothManager constructor enter.");
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Ensure that the device supports use of BT SCO audio for off call use cases.
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            log.e("Bluetooth SCO audio is not available off call");
            return;
        }

        audioDeviceContext = context;
        audioDevice = defaultAudioDevice;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            log.w("Device does not support Bluetooth");
            return;
        }

        setBluetoothState(State.SCO_DISCONNECTED);
        registerBtReceiver();
        audioManager.setBluetoothScoOn(true);
        audioManagerStartBluetoothSCO();
        log.d("BluetoothManager constructor exit.");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        audioManager.setBluetoothScoOn(false);
        if (null != bluetoothProfile && bluetoothAdapter != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothProfile);
        }
    }

    void onPause() {
        log.d("onPause() enter.");
        log.d("onPause() exit.");
    }

    void onResume() {
        log.d("onResume() enter.");

        /*This is required to force a second call to BluetoothProfile.ServiceListener.onServiceConnected()
        in case the user turns OFF Bluetooth from the settings and later turns back ON. First call to
        BluetoothProfile.ServiceListener.onServiceConnected() is invoked as soon as the Phone finds the
        paired Bluetooth headset but BluetoothProfile returns empty device list. Forcing a second call works.
        */
        forceInvokeConnectBluetooth();

        log.d("onResume() exit.");
    }

    void registerBtReceiver() {
        log.d("isBluetoothBroadcastReceiverRegistered = " + isBluetoothBroadcastReceiverRegistered);

        if (isBluetoothBroadcastReceiverRegistered) {
            return;
        }

        log.d("registerBtReceiver() enter.");
        IntentFilter bluetoothHeadsetFilter = new IntentFilter();
        bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        bluetoothHeadsetFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        audioDeviceContext.registerReceiver(bluetoothBroadcastReceiver, bluetoothHeadsetFilter);

        isBluetoothBroadcastReceiverRegistered = true;
        log.d("registerBtReceiver() exit");
    }


    synchronized public void unregisterBtReceiver() {
        log.d("isBluetoothBroadcastReceiverRegistered = " + isBluetoothBroadcastReceiverRegistered);

        if (!isBluetoothBroadcastReceiverRegistered) {
            return;
        }
        log.d("unregisterBtReceiver() enter");

        audioDeviceContext.unregisterReceiver(bluetoothBroadcastReceiver);
        isBluetoothBroadcastReceiverRegistered = false;
        log.d("unregisterBtReceiver() exit");
    }

    synchronized void startBluetoothSCO() {
        if (audioDevice.isWiredHeadSetConnected()) {
            log.e("Bluetooth cannot be turned on when wired headset is connected.");
            return;
        }
        setConnectionAttempts(0);

        tryStartBluetoothSCO();
    }

    synchronized void setConnectionAttempts(int attempts) {
        scoConnectionAttempts = attempts;
        log.d("setConnectionAttempts(). scoConnectionAttempts set to " + attempts);
    }

    synchronized void tryStartBluetoothSCO() {
        log.d("tryStartBluetoothSCO() enter <-");
        if (bluetoothConnectionState == State.SCO_CONNECTED || bluetoothConnectionState == State.SCO_CONNECT_ATTEMPT)
        {
            return;
        }
        setBluetoothState(State.SCO_CONNECT_ATTEMPT);
        // In case this is not the first attempt to connect, we include
        // an exponential back-off retry mechanism (e.g., 50, 100, 200... ms).
        // This is requirement for some particular devices (Huawei, Pixel...).
        // See VIDCS-767 and OPENTOK-48976 for a discussion.
        new Handler().postDelayed(() -> {
            if (bluetoothConnectionState == State.SCO_CONNECTED)
            {
                setConnectionAttempts(0);
                return;
            }
            try {
                audioManagerStopBluetoothSCO();
                audioManagerStartBluetoothSCO();
            } catch (NullPointerException e) {
                log.e("tryStartBluetoothSco(): " + e.getMessage());
            }
        }, (long) (scoConnectionAttempts == 0 ? 0 : SCO_CONNECTION_ATTEMPT_DELAY * Math.pow(2, scoConnectionAttempts)));

        setConnectionAttempts(scoConnectionAttempts+1);
        log.d("tryStartBluetoothSCO() exit ->");
    }

    private void audioManagerStartBluetoothSCO()
    {
        log.d("audioManagerStartBluetoothSCO() enter <-");
        audioManager.startBluetoothSco();
        log.d("audioManagerStartBluetoothSCO() exit <-");
    }

    synchronized void stopBluetoothSCO() {
        log.d("stopBluetoothSCO() enter <-");
        setConnectionAttempts(-1);

        // Do nothing if a connection attempt is scheduled
        if (bluetoothConnectionState == State.SCO_CONNECT_ATTEMPT)
            return;

        try {
            audioManagerStopBluetoothSCO();
        } catch (NullPointerException e) {
            log.e("stopBluetoothSco(): " + e.getMessage());
        }

        log.d("stopBluetoothSCO() exit ->");
    }

    synchronized void audioManagerStopBluetoothSCO() {
        log.d("audioManagerStopBluetoothSCO() enter <-");
        audioManager.stopBluetoothSco();
        log.d("audioManagerStopBluetoothSCO() exit ->");
    }

    boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (audioDeviceContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                log.e("Audio may not route through paired Bluetooth device unless the " +
                        "bluetooth permission has been granted explicitly in the App settings.");
                return false;
            }
        }
        return true;
    }

    //This happens when user turns ON/OFF the Bluetooth Radio from phone settings.
    private final BluetoothProfile.ServiceListener bluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            synchronized(BluetoothManager.this) {
                if (!hasBluetoothPermission()) {
                    return;
                }
                log.d("BluetoothProfile.ServiceListener.onServiceConnected() enter.");
                if (BluetoothProfile.HEADSET == profile) {
                    bluetoothProfile = proxy;
                    List<BluetoothDevice> devices = proxy.getConnectedDevices();
                    if (!devices.isEmpty() &&
                            BluetoothHeadset.STATE_CONNECTED == proxy.getConnectionState(devices.get(0))) {
                        bluetoothDevice = devices.get(0);
                        log.d("Connected to bluetooth headset " + bluetoothDevice.getName());

                        // Force a init of bluetooth: the handler will not send a connected event if a
                        // device is already connected at the time of proxy connection request.
                        Intent intent = new Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                        intent.putExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_CONNECTED);
                        bluetoothBroadcastReceiver.onReceive(audioDeviceContext, intent);
                    } else {
                        log.d("No bluetooth headset connected.");
                    }
                } else {
                    log.d("profile = " + profile);
                }
                log.d("BluetoothProfile.ServiceListener.onServiceConnected() exit.");
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            synchronized(BluetoothManager.this) {
                log.d("BluetoothProfile.ServiceListener.onServiceDisconnected() enter.");
                if (profile != BluetoothProfile.HEADSET) {
                    return;
                }
                bluetoothDevice = null;
                bluetoothProfile = null;
                log.d("BluetoothProfile.ServiceListener.onServiceDisconnected() exit.");
            }
        }
    };

    synchronized public boolean isBluetoothConnected() {
        boolean isConnected = (bluetoothDevice != null);
        log.d("isBluetoothConnected = " + isConnected);
        return isConnected;
    }

    synchronized AdvancedAudioDevice.BluetoothState getBluetoothState() {
        log.d("getBluetoothState(). Current bluetoothState = " + bluetoothConnectionState);
        return bluetoothConnectionState == State.SCO_CONNECTED ?
                AdvancedAudioDevice.BluetoothState.Connected :
                AdvancedAudioDevice.BluetoothState.Disconnected ;
    }

    synchronized void setBluetoothState(State state) {
        bluetoothConnectionState = state;
        log.d("setBluetoothState(). bluetoothState set to " + bluetoothConnectionState);
    }

    synchronized void forceInvokeConnectBluetooth() {
        if (audioDevice == null) {
            return;
        }
        log.d("forceInvokeConnectBluetooth() enter");
        // Force reconnection of bluetooth in the event of a phone call.
        if (bluetoothAdapter != null) {
            bluetoothAdapter.getProfileProxy(
                    audioDeviceContext,
                    bluetoothProfileServiceListener,
                    BluetoothProfile.HEADSET
            );
        }
        log.d("forceInvokeConnectBluetooth() exit");
    }

    final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized(BluetoothManager.this) {
                final String action = intent.getAction();
                if (null == action) {
                    log.e("bluetoothBroadcastReceiver.onReceive(): error !!");
                    return;
                }
                switch (action) {
                    case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: {
                        final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                        switch (state) {
                            case BluetoothHeadset.STATE_CONNECTED:
                                log.d("bluetoothBroadcastReceiver.onReceive(): BluetoothHeadset.STATE_CONNECTED");
                                startBluetoothSCO();
                                break;
                            case BluetoothHeadset.STATE_DISCONNECTING:
                                log.d("bluetoothBroadcastReceiver.onReceive(): BluetoothHeadset.STATE_DISCONNECTING");
                                break;
                            case BluetoothHeadset.STATE_DISCONNECTED:
                                log.d("bluetoothBroadcastReceiver.onReceive(): BluetoothHeadset.STATE_DISCONNECTED");
                                stopBluetoothSCO();
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                    case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED: {
                        final int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                        switch (state) {
                            case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                                log.d("bluetoothBroadcastReceiver.onReceive(): AudioManager.SCO_AUDIO_STATE_CONNECTED");
                                setBluetoothState(State.SCO_CONNECTED);
                                if (scoConnectionAttempts == -1) {
                                    stopBluetoothSCO();
                                } else {
                                    setConnectionAttempts(0);
                                    audioManager.setSpeakerphoneOn(false);
                                }
                                break;
                            case AudioManager.SCO_AUDIO_STATE_ERROR:
                                log.d("bluetoothBroadcastReceiver.onReceive(): AudioManager.SCO_AUDIO_STATE_ERROR");
                                break;
                            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                                log.d("bluetoothBroadcastReceiver.onReceive(): AudioManager.SCO_AUDIO_STATE_DISCONNECTED");
                                if (isInitialStickyBroadcast()) {
                                    log.d("Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.");
                                    return;
                                }
                                if (scoConnectionAttempts == -1 || scoConnectionAttempts >= MAX_SCO_CONNECTION_ATTEMPTS) {
                                    setBluetoothState(State.SCO_DISCONNECTED);
                                    audioDevice.restoreAudioAfterBluetoothDisconnect();
                                } else {
                                    log.d("bluetoothBroadcastReceiver.onReceive(): Forcing " +
                                            "bluetooth reconnection as it was unexpectedly " +
                                            "disconnected, attempt: " + scoConnectionAttempts);
                                    setBluetoothState(State.SCO_CONNECTING);
                                    tryStartBluetoothSCO();
                                }
                                break;
                            case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                                log.d("bluetoothBroadcastReceiver.onReceive(): AudioManager.SCO_AUDIO_STATE_CONNECTING");
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                }
            }
        }
    };
}
