# Basic Video Chat with `ConnectionService`

This project enables real-time video calling using a self-managed ConnectionService. The app handles outgoing and incoming calls, integrates with system Telecom APIs, and manages in-call notifications and audio routing.

## Features

- Outgoing and incoming VoIP calls with fullscreen call UIs
- Integration with Android Telecom API for call and notification management
- Real-time video sessions using OpenTok
- Audio device selection and call hold capabilities
- Self-managed calls for a streamlined calling experience

## How It Works

The app customizes Android’s ConnectionService and Connection classes to:
- Place outgoing calls through a tailored PhoneAccount.
- Report incoming calls.
- Manage call state changes, notifications, foreground execution handling and audio routing.
- Hardcoded local OpenTok credentials.

## Configuration

1. **API Credentials:**  
   Update your `OpenTokConfig` with your `API_KEY`, `SESSION_ID`, and `TOKEN` variables. You can obtain these values from your [TokBox account](https://tokbox.com/account/#/) by creating a new session in the [Video API Playground](https://tokbox.com/developer/tools/playground/) site. 
    In a production setup, these values should be provided from a secure server. 

2. **PhoneAccount:**  
   The `PhoneAccountManager` registers the PhoneAccount necessary to interface with the Telecom API.

3. **Manifest Setup:**  
   Verify that `AndroidManifest.xml` includes all necessary permissions such as `CAMERA`, `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `MANAGE_OWN_CALLS`, and `BIND_TELECOM_CONNECTION_SERVICE`.

Register the service in `AndroidManifest.xml` file.

```xml
        <service
    android:name=".connectionservice.VonageConnectionService"
    android:exported="true"
    android:permission="android.permission.BIND_TELECOM_CONNECTION_SERVICE"
    android:foregroundServiceType="microphone|camera">
    <intent-filter>
        <action android:name="android.telecom.ConnectionService" />
    </intent-filter>
</service>
```

## Requirements

- Android API Level 26 or higher is recommended.
- Ensure proper runtime permissions are granted for camera, audio recording, and foreground service usage.

## Overview

A `ConnectionService` allows apps to manage VoIP or phone calls, whether they need to integrate with 
the system dialer (system managed) or operate independently (self managed). By implementing this 
service, a VoIP app can leverage Android’s Telecom APIs to provide features such as call switching, 
unified audio route management, Bluetooth device support, and integration with companion devices 
like smartwatches. This ensures calls are accessible and controllable across different devices, with
consistent user experiences for actions like answering, rejecting, or ending calls.

## TelecomManager and PhoneAccount

To initiate phone or VoIP calls, `TelecomManager` relies on a registered `PhoneAccount`. Register 
your app’s `PhoneAccount` using `TelecomManager.registerPhoneAccount()`, and assign it the 
`CAPABILITY_SELF_MANAGED` capability. This signals that your app will handle the connection logic 
and call management independently.

## Implementing ConnectionService

To handle outgoing and incoming calls, your app should use [TelecomManager.placeCall(Uri, Bundle)](https://developer.android.com/reference/android/telecom/TelecomManager#placeCall(android.net.Uri,%20android.os.Bundle)) for outgoing calls and [TelecomManager.addNewIncomingCall()](https://developer.android.com/reference/android/telecom/TelecomManager#addNewIncomingCall(android.telecom.PhoneAccountHandle,%20android.os.Bundle)) to notify the system of new incoming calls. When these APIs are called, the Telecom framework binds to your app’s `ConnectionService`.

Your implementation should override the following `ConnectionService` methods:
- [onCreateOutgoingConnection()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateOutgoingConnection(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)): Invoked by Telecom to create a new [Connection](https://developer.android.com/reference/android/telecom/Connection) for an outgoing call initiated by your app.
- [onCreateOutgoingConnectionFailed()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateOutgoingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)): Called if an outgoing call cannot be processed. Your app should not attempt to place the call.
- [onCreateIncomingConnection()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnection(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)): Invoked to create a new [Connection](https://developer.android.com/reference/android/telecom/Connection) for an incoming call reported by your app.
- [onCreateIncomingConnectionFailed()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)): Called if an incoming call cannot be handled. Your app should not show a notification and should silently reject the call.

## Implementing [Connection](https://developer.android.com/reference/android/telecom/Connection)

To represent calls in your app, extend the [Connection](https://developer.android.com/reference/android/telecom/Connection) class. When creating a new `Connection` instance to return from your [`ConnectionService`](https://developer.android.com/reference/android/telecom/ConnectionService), make sure to configure these properties:
- Use `Connection#setAddress(Uri, int)` to specify the other party’s identifier. For phone calls, this should be a `PhoneAccount#SCHEME_TEL` URI.
- Set the display name with `Connection#setCallerDisplayName(String, int)`, which will appear on Bluetooth and wearable devices—especially important if no phone number is used.
- Apply `Connection#PROPERTY_SELF_MANAGED` via `Connection#setConnectionProperties(int)` to indicate your app manages the call.
- If your app supports call hold, set `Connection#CAPABILITY_SUPPORT_HOLD` and `Connection#CAPABILITY_HOLD` using `Connection#setConnectionCapabilities(int)` to enable concurrent call scenarios.
- Call `Connection#setAudioModeIsVoip(true)` to inform the platform that the call is VoIP.
- Do not change the call state (e.g., with `Connection#setActive()` or `Connection#setOnHold()`) until the `Connection` has been added to Telecom by returning it from `onCreateOutgoingConnection` or `onCreateIncomingConnection`.