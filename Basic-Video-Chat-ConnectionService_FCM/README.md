# `ConnectionService` with Notifications Push powered by Firebase Cloud Messaging

The sample app enables real-time video calling between users, featuring both push-notification-initiated calls and simulated calls for testing. It uses Firebase Cloud Messaging (FCM) to send and receive call invitations and leverages Android SDK to handle the live video feed.

Key Features:
 - Push Notification Call Invites using FCM:
    - between two clients.
    - via Postman to your client.
 - Call Simulation Mode for testing call flows without push
 - Handle incoming calls

## Overview

An abstract service that should be implemented by any apps which either:
- Can make phone calls (VoIP or otherwise) and want those calls to be integrated into the built-in phone app.  Referred to as a <b>system managed</b> [ConnectionService](https://developer.android.com/reference/android/telecom/ConnectionService).
- Are a standalone calling app and don't want their calls to be integrated into the built-in phone app.  Referred to as a <b>self managed</b> [ConnectionService](https://developer.android.com/reference/android/telecom/ConnectionService).

A VoIP app can implement a [`ConnectionService`](https://developer.android.com/reference/android/telecom/ConnectionService) to ensure that its calls are integrated into the Android platform.  There are numerous benefits to using the Telecom APIs for a VoIP app:
- Call concurrency is handled - the user is able to swap between calls in different apps and on the mobile network.
- Simplified audio routing - the platform provides your app with a unified list of the audio routes which are available and a standardized way to switch audio routes.
- Bluetooth integration - your calls will be visible on and controllable via bluetooth devices.
- Companion device integration - wearable devices such as watches which implement an `InCallService` can optionally subscribe to see self-managed calls.  Similar to a bluetooth headunit, wearables will typically render your call using a generic call UX and provide the user with basic call controls such as hangup, answer, reject.

## Manifest declarations and permissions

To integrate a self-managed ConnectionService, declare the `MANAGE_OWN_CALLS` and `BIND_TELECOM_CONNECTION_SERVICE` permissions in the AndroidManifest.xml file.

```xml
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
<uses-permission android:name="android.permission.BIND_TELECOM_CONNECTION_SERVICE" />

```

Register the service in `AndroidManifest.xml` file.

```xml
<service
    android:name=".MyConnectionService"
    android:label="@string/service_label"
    android:permission="android.permission.BIND_TELECOM_CONNECTION_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.ConnectionService" />
    </intent-filter>
</service>
```

## TelecomManager and PhoneAccount

[TelecomManager](https://developer.android.com/reference/android/telecom/TelecomManager) uses a registered [PhoneAccount](https://developer.android.com/reference/android/telecom/PhoneAccount) to place a phone/VoIP call. Use [TelecomManager.registerPhoneAccount()](https://developer.android.com/reference/android/telecom/TelecomManager#registerPhoneAccount(android.telecom.PhoneAccount)) and configure a PhoneAccount with [CAPABILITY_SELF_MANAGED](https://developer.android.com/reference/android/telecom/PhoneAccount#CAPABILITY_SELF_MANAGED), indicating that this PhoneAccount is responsible for managing its own Connection.

## Implement ConnectionService

Your app uses [TelecomManager.placeCall(Uri, Bundle)](https://developer.android.com/reference/android/telecom/TelecomManager#placeCall(android.net.Uri,%20android.os.Bundle)) to start new outgoing calls and
[TelecomManager.addNewIncomingCall()](https://developer.android.com/reference/android/telecom/TelecomManager#addNewIncomingCall(android.telecom.PhoneAccountHandle,%20android.os.Bundle)) to report new incoming
calls. Calling these APIs causes the Telecom stack to bind to your app's
ConnectionService implementation.
Your app should implement the following `ConnectionService` methods:
- [onCreateOutgoingConnection()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateOutgoingConnection(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)) - called by Telecom to ask your app to make a new [Connection](https://developer.android.com/reference/android/telecom/Connection)
    to represent an outgoing call your app requested via
    [TelecomManager.placeCall(Uri, Bundle)](https://developer.android.com/reference/android/telecom/TelecomManager#placeCall(android.net.Uri,%20android.os.Bundle)).
-<[onCreateIncomingConnectionFailed()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)) - called by Telecom to inform your app that a call it reported via
    [TelecomManager.placeCall(Uri, Bundle)](https://developer.android.com/reference/android/telecom/TelecomManager#placeCall(android.net.Uri,%20android.os.Bundle)) cannot be handled at this time.  Your app
    should NOT place a call at the current time.
-[onCreateIncomingConnection()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnection(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)) - called by Telecom to ask your app to make a new [Connection](https://developer.android.com/reference/android/telecom/Connection)
    to represent an incoming call your app reported via
    [TelecomManager.addNewIncomingCall()](https://developer.android.com/reference/android/telecom/TelecomManager#addNewIncomingCall(android.telecom.PhoneAccountHandle,%20android.os.Bundle)).
-[onCreateIncomingConnectionFailed()](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)) - called by Telecom to inform your app that an incoming call it reported
    via [TelecomManager.addNewIncomingCall()](https://developer.android.com/reference/android/telecom/TelecomManager#addNewIncomingCall(android.telecom.PhoneAccountHandle,%20android.os.Bundle)) cannot be handled
    at this time.  Your app should NOT post a new incoming call notification and should silently
    reject the call.

## Implement [Connection](https://developer.android.com/reference/android/telecom/Connection)

Your app should extend the [Connection](https://developer.android.com/reference/android/telecom/Connection) class to represent calls in your app.  When you
create new instances of your [Connection](https://developer.android.com/reference/android/telecom/Connection), you should ensure the following properties are
set on the new [Connection](https://developer.android.com/reference/android/telecom/Connection) instance returned by your [`ConnectionService`](https://developer.android.com/reference/android/telecom/ConnectionService):
- `Connection#setAddress(Uri, int)` - the identifier for the other party.  For
    apps that user phone numbers the `Uri` can be a `PhoneAccount#SCHEME_TEL` URI
    representing the phone number.
- `Connection#setCallerDisplayName(String, int)` - the display name of the other
    party.  This is what will be shown on Bluetooth devices and other calling surfaces such
    as wearable devices.  This is particularly important for calls that do not use a phone
    number to identify the caller or called party.
- `Connection#setConnectionProperties(int)` - ensure you set
    `Connection#PROPERTY_SELF_MANAGED` to identify to the platform that the call is
    handled by your app.
- `Connection#setConnectionCapabilities(int)` - if your app supports making calls
    inactive (i.e. holding calls) you should get `Connection#CAPABILITY_SUPPORT_HOLD` and
    `Connection#CAPABILITY_HOLD` to indicate to the platform that you calls can potentially
    be held for concurrent calling scenarios.
- `Connection#setAudioModeIsVoip(boolean)` - set to `true` to ensure that the
    platform knows your call is a VoIP call.
- For newly created [Connection](https://developer.android.com/reference/android/telecom/Connection) instances, do NOT change the state of your call
    using `Connection#setActive()`, `Connection#setOnHold()` until the call is added
    to Telecom (ie you have returned it via
    `ConnectionService#onCreateOutgoingConnection(PhoneAccountHandle, ConnectionRequest)`
    or
    `ConnectionService#onCreateIncomingConnection(PhoneAccountHandle, ConnectionRequest)`).

## Firebase Cloud Messaging (FCM) - Android Integration Guide

## What is FCM?

[Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging) is a free, cross-platform messaging solution from Google that lets you **send notifications and data messages to Android, iOS, and web clients**. It is widely used for push notifications, real-time updates, and messaging apps.

## How Does FCM Work?

FCM uses a **publish-subscribe model** between:

1. **Your App (Client)** – Receives messages from Firebase.
2. **Firebase Cloud Messaging Server** – Distributes messages.
3. **Your App Server (Optional)** – Sends targeted messages via FCM APIs.

## Types of Messages

- **Notification Messages**: Handled automatically by FCM when the app is in the background.
- **Data Messages**: Handled by your app regardless of foreground/background state. You define the payload and behavior.

## How to Set Up FCM in an Android Project

### Prerequisites

- A Firebase account: [https://firebase.google.com/](https://firebase.google.com/)

---

### Add Firebase to Your Android Project

#### Create a Project on Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Register your Android app with your package name
4. Download the `google-services.json` file
5. Place it in your project's `app/` directory

---

### Update Gradle Files

#### Root `build.gradle`

```groovy
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}
```

### Get Google Cloud Token

In your server you need to dynamically fetch the google cloud token. For the scope of this sample app,
you can hardcode it to the project in **getGoogleCloudToken()**. To retrieve the token you can use:

```python
from google.oauth2 import service_account
from google.auth.transport.requests import Request

SCOPES = ["https://www.googleapis.com/auth/cloud-platform"]
SERVICE_ACCOUNT_FILE = "/path/to/googleservices.json" # Can be downloaded from your Google Cloud project page

credentials = service_account.Credentials.from_service_account_file(
    SERVICE_ACCOUNT_FILE,
    scopes=SCOPES
)

# Refresh the token if necessary
if not credentials.valid:
    credentials.refresh(Request())

print(credentials.token)
```

### Example of JSON data

The data sent between endpoints its customiseable. It should contain the FCM token of the receiver, it can contain values for a notification message and any additional values under "data".

```json
{
   "message": {
       "token": "<DEVICE_TOKEN>",
       "notification": {
           "title": "Incoming Call",
           "body": "Mom is calling..."
       },
       "data": {
           "type": "INCOMING_CALL",
           "callerId": "user123",
           "callerName": "Mom"
       }
   }
}
```

# Configure the app 
Open the `OpenTokConfig` file and configure the `API_KEY`, `SESSION_ID`, and `TOKEN` variables. You can obtain these values from your [TokBox account](https://tokbox.com/account/#/).

### (Optional) Deploy a back end web service

 For a production application, the `SESSION_ID` and `TOKEN` values must be generated by your app server application and passed to the client, because:
 - credentials would expire after a certain amount of time
 - credentials are lined to given session (all users would be connected to the same room)
 
To quickly deploy a pre-built server click at one of the Heroku buttons below. You'll be sent to Heroku's website and prompted for your OpenTok `API Key` and `API Secret` — you can obtain these values on your project page in your [TokBox account](https://tokbox.com/account/user/signup). If you don't have a Heroku account, you'll need to sign up (it's free).

| PHP server  | Node.js server|
| ------------- | ------------- |
| <a href="https://heroku.com/deploy?template=https://github.com/opentok/learning-opentok-php" target="_blank"> <img src="https://www.herokucdn.com/deploy/button.png" alt="Deploy"></a>  | <a href="https://heroku.com/deploy?template=https://github.com/opentok/learning-opentok-node" target="_blank"> <img src="https://www.herokucdn.com/deploy/button.png" alt="Deploy"></a>  |
| [Repository](https://github.com/opentok/learning-opentok-php) | [Repository](https://github.com/opentok/learning-opentok-node) |

> Note: You can also build your server from scratch using one of the [server SDKs](https://tokbox.com/developer/sdks/server/).

After deploying the server open the `ServerConfig` file in this project and configure the `CHAT_SERVER_URL` with your domain to fetch credentials from the server:

```java
public static final String CHAT_SERVER_URL = "https://YOURAPPNAME.herokuapp.com";
```

> Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a valid URL.

This is the code responsible for retrieving the credentials from web server:

```java
private void getSession() {
    Log.i(TAG, "getSession");

    Call<GetSessionResponse> call = apiService.getSession();

    call.enqueue(new Callback<GetSessionResponse>() {
        @Override
        public void onResponse(Call<GetSessionResponse> call, Response<GetSessionResponse> response) {
            GetSessionResponse body = response.body();
            initializeSession(body.apiKey, body.sessionId, body.token);
        }

        @Override
        public void onFailure(Call<GetSessionResponse> call, Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    });
}
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
