OpenTok Android SDK Beta
============================

The OpenTok Android SDK 2.2 Beta lets you use OpenTok on WebRTC video sessions in apps you build for Android devices.

Apps written with the OpenTok Android SDK can interact with OpenTok apps written with the following OpenTok SDKs:

* OpenTok.js 2.0
* OpenTok.js 2.2
* OpenTok iOS SDK 2.2 Beta

This is a beta version. See "Changes to the API" and "New API" below for more information on changes
in this version. The API may change in upcoming releases.

Installation
------------

The library binaries are included in the OpenTok/libs subdirectory of the SDK.

Developer and client requirements
---------------------------------

* While in beta, the OpenTok Android SDK is expected to work with Android 4.0+ and devices from
Samsung, Google Nexus, Motorola Moto, and LG Optimus families. If you are targeting a different OS
version or device family, then please contact us at support@tokbox.com.

* You need an [OpenTok developer account](https://dashboard.tokbox.com/).

Using the sample apps
---------------------

The samples directory contains the OpenTokSamples app. This app shows the most basic
functionality of the OpenTok Android SDK: connecting to sessions, publishing streams, and subscribing to streams. It also shows how to add UI controls to publisher and subscriber views
and how to use custom video capturer and renderers.

For more information, see the README file in the samples directory.

Creating your own app using the OpenTok Android SDK
---------------------------------------------------

Add the following libraries to your project's build path:

* opentok-android-sdk-2.2.jar
* armeabi/libopentok.so

These are included in the OpenTok/libs subdirectory of the SDK. (From the desktop, drag the
opentok-android-sdk-2.2.jar file and armeabi directory into the libs directory of your project
in the ADT package explorer.)

Also, you need to add the following permissions and features to your app manifest:

* android.permission.CAMERA
* android.permission.INTERNET
* android.permission.RECORD_AUDIO
* android.permission.WAKE_LOCK
* android.permission.MODIFY_AUDIO_SETTINGS
* android.hardware.camera
* android.hardware.camera.autofocus

Your app needs to use a session ID and token generated with your OpenTok API key, which you can get
at [the OpenTok developer dashboard](https://dashboard.tokbox.com).

For test purposes, you can generate a session ID and token at
[the projects page](https://dashboard.tokbox.com/projects) of the OpenTok developer dashboard.
For a production app, generate unique tokens (and session IDs, if you need to support multiple
sessions) using the [OpenTok server-side libraries](http://tokbox.com/opentok/libraries/server/).

Changes to the API
------------------

Changes in the OpenTok Andriod SDK 2.2 Beta:

* The Session() constructor now includes an apiKey parameter:

      Session(Context context, String apiKey, String sessionId)

* The Session.connect() method no longer has an apiKey parameter:

      void connect(final String token)

* The listener classes and method names have changed:

        Session.Listener.addPublisher --> Session.PublisherListener.onPublisherAdded
        Session.Listener.connected --> Session.SessionListener.onConnected
        Session.Listener.connectionCreated --> Session.ConnectionListener.onConnectionCreated
        Session.Listener.connectionDestroyed --> Session.ConnectionListener.onConnectionDestroyed
        Session.Listener.disconnected --> Session.SessionListener.onDisconnected
        Session.Listener.droppedStream --> Session.SessionListener.onStreamDropped
        Session.Listener.error --> Session.SessionListener.onError
        Session.Listener.onSignal --> Session.SignalListener.onSignalReceived
        Session.Listener.receivedStream --> Session.SessionListener.onStreamReceived
        Session.Listener.removePublisher --> Session.PublisherListener.onPublisherRemoved
        Session.Listener.streamChangeHasAudio --> Session.StreamPropertiesListener.onStreamHasAudioChanged
        Session.Listener.streamChangeHasVideo --> Session.StreamPropertiesListener.onStreamHasVideoChanged
        Session.Listener.streamChangeVideoDimensions --> Session.StreamPropertiesListener.onStreamVideoDimensionsChanged

        PublisherKit.Listener.changedCamera --> Publisher.CameraListener.onCameraChanged
        PublisherKit.Listener.streamCreated --> PublisherKit.PublisherListener.onStreamCreated
        PublisherKit.Listener.streamDestroyed --> PublisherKit.PublisherListener.onStreamDestroyed
        PublisherKit.Listener.error --> PublisherKit.PublisherListener.onError

        Subscriber.Listener.connected --> SubscriberKit.SubscriberListener.onConnected
        Subscriber.Listener.disconnected --> SubscriberKit.SubscriberListener.onDisconnected
        Subscriber.Listener.videoDisabled --> SubscriberKit.VideoListener.onVideoDisabled
        Subscriber.Listener.error --> SubscriberKit.SubscriberListener.onError
        Subscriber.Listener.videoDataReceived --> SubscriberKit.VideoListener.onVideoDataReceived

* Listeners have been removed as parameters of the constructors for the Session, Publisher and
  Subscriber classes. To add an event listener, call one of the following methods:

        Session.setArchiveListener(Session.ArchiveListener listener)
        Session.setConnectionListener(Session.ConnectionListener listener)
        Session.setPublisherListener(Session.PublisherListener listener)
        Session.setSessionListener(Session.SessionListener listener)
        Session.setSignalListener(Session.SignalListener listener)
        Session.setStreamPropertiesListener(Session.StreamPropertiesListener listener)
        Publisher.setCameraListener(Publisher.CameraListener listener)
        PublisherKit.setPublisherListener(PublisherKit.PublisherListener listener)
        SubscriberKit.setSubscriberListener(SubscriberKit.SubscriberListener listener)
        SubscriberKit.setVideoListener(SubscriberKit.VideoListener listener)

  You now only need to add listeners for events that you are interested in. For example,
  if your app does not use the signaling or archiving features, then you need not add
  Session.ArchiveListener or Session.SignalListener listeners.

* The hasAudio parameter of the Session.StreamPropertiesListener.onStreamHasAudioChanged() method
  is now a Boolean value.

* The hasVideo parameter of the Session.StreamPropertiesListener.onStreamHasVideoChanged() method is
  now a Boolean value.

Changes in the OpenTok Android SDK 2.0 Beta 2:

* The Publisher.getStreamId() method has been replaced with the Publisher.getStream() method.

* The Subscriber.newInstance() method has been replaced by the Subscriber() constructor (which the
  Subscriber class inherits from the SubscriberKit class).

* Session.Listener.receivedStream() and droppedStream() methods are now only called for streams
  published by other clients. For streams published by your client, the
  PublisherKit.Listener.streamCreated() and PublisherKit.Listener.streamDestroyed() are called.

* The Publisher.Listener onPublisherStreamingStarted() and onPublisherStreamingStopped() methods
  are replaced with streamCreated() and streamDestroyed().

* Methods of the Listener classes (Session.Listener, PublisherKit.Listener, SubscriberKit.Listener)
  have new names. For example, Session.Listener.onSessionConnected() is now
  Session.Listener.connected() ("onSession" has been removed from the beginning of the name).
  Also, the first parameter of each method of each Listener classes is the object that the event
  pertains to (Session, PublisherKit, or SubscriberKit). For details, see the reference
  documentation in the docs subdirectory.

* The getListener() method has been removed from the Publisher, Session, and Subscriber classes.

New API
-------

### New API in the OpenTok Android SDK 2.2 Beta:

The new Session.ArchiveListener class defines a listener for archive-related events in
the session. These include events when an archive recording starts and when an archive
recording stops. In response to these callbacks, you may want to add a user interface
notification (such as an icon in the Publisher view) that the session is (or is not) being
recorded.

Add an event listener for archive-related events by calling:

    Session.setArchiveListener(Session.ArchiveListener listener)

OpenTok 2.0 archiving is currently in beta. For more information, see
<http://tokbox.com/platform#archiving>.

The PublisherKit, Publisher, Session, and SubscriberKit classes include protected callback
methods for events. To process events, you can extend these classes and override these
methods instead of overriding methods of the Listener interfaces.

### New API in the OpenTok Android SDK 2.0 Beta 2:

The new classes and methods in this version support new capabilities. For details on
the new API, see the reference documentation in the docs directory.

#### Custom video capturers and renderers

The following new classes support custom video capturers and renderers:

* PublisherKit -- Use this class to use a custom video capturer and video renderer for
  an audio-video stream to publish to an OpenTok session. Note that the Publisher class,
  which uses the iOS camera as a direct video feed, is a subclass of PublisherKit.

* SubscriberKit -- Use this class to use a custom video renderer for an audio-video stream.
  Note that the Subscriber class, which displays the video stream unaltered, is a subclass
  of SubscriberKit.

* BaseVideoCapturer -- Use this interface is to provide video data to an PublisherKit object.
  You can extend this class to create your own custom video capturer.

* BaseVideoRenderer -- Use this interface is to render video data in an PublisherKit
  object or SubscriberKit object. Use the BaseVideoCapturer.CaptureSettings class to define
  settings for the video renderer.

#### Signaling API

The following methods support the new signaling API:

    Session.sendSignal(type, data)
    Session.sendSignal(type, data, connections)
    Session.Listener.onSignal(session, type, data, connection)

#### Pause and resume notifications

The Session includes onPause() and onResume() methods, which are called when the main activity
pauses and resumes. Create a subclass of the Session class to override these methods, to define
behavior when the activity pauses or resumes. (For example, in the onPause() method, you may want
to disable video but not audio of streams in the session.)

#### New runtime errors

See the documentation for the OpentokError.ErrorCode enum.

#### New SubscriberKit.Listener.disconnected() method

The SubscriberKit.Listener.disconnected() method is called when the subscriber's stream leaves
the session.

#### New Session.Listener addPublisher() and removePublisher() methods

The Session.Listener addPublisher() method is called when the client starts publishing a steam to
the session. The removePublisher() method is called when the client stops publishing a steam to
the session.

Known issues
------------

* You cannot publish streams in the ADT Simulator. Build and deploy to a supported device.

Bug Fixes
---------

* If an Android device rotates while publishing a stream to a session being archived, the video
  orientation in the archive recording adjusts accordingly. (For information on archiving, see
  <http://tokbox.com/platform#archiving>.)

Connect with TokBox and with other OpenTok developers
-----------------------------------------------------

Your comments and questions are welcome. Come join the conversation at the
[OpenTok Android SDK forum](http://www.tokbox.com/forums/android).

More information
----------------

See the reference documentation in the docs directory.

For more information on OpenTok, go to <http://www.tokbox.com/>.
