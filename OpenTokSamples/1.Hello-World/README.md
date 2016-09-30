# Project 1: Hello World

## Testing the app module

1. Configure the project to use your own OpenTok session and token. If you don't have an OpenTok
   API key yet, [sign up for a Developer Account](https://dashboard.tokbox.com/signups/new).
   Then to generate a test session ID and token, use the Project Tools on the
   [Project Details](https://dashboard.tokbox.com/projects) page.

   Open the OpenTokConfig.java file and set the `SESSION_ID`, `TOKEN`, and `API_KEY` strings
   to your own session ID, token, and API key respectively. The OpenTokConfig class is in the
   com.opentok.android.demo.config package.

   For more information, see the OpenTok [Session Creation
   Overview](https://tokbox.com/opentok/tutorials/create-session/) and the [Token Creation
   Overview](https://tokbox.com/opentok/tutorials/create-token/).

2.  Connect your Android device to a USB port on your computer. Set up
    [USB debugging](http://developer.android.com/tools/device.html) on your device.

3.  Run the app on your device.

    Once the app connects to the OpenTok session, it publishes an audio-video stream, which is
    displayed onscreen. Then, the same audio-video stream shows up as a subscribed stream
    (along with any other streams currently in the session).

5.  Close the app. Now set up the app to subscribe to audio-video streams other than your own:

    -   In the OpenTokConfig class change the `SUBSCRIBE_TO_SELF` property to be set to `false`.
    -   Edit browser_demo.html located in the root directory of this project, and modify the variables `apiKey`, `sessionId`,
        and `token` with your OpenTok API Key, and with the matching session ID and token. (Note that you would normally use
        the OpenTok server-side libraries to issue unique tokens to each client in a session. But for testing purposes,
        you can use the same token on both clients. Also, depending on your app, you may use the OpenTok server-side
        libraries to generate new sessions.)
    -   Add the browser_demo.html file to a web server. (You cannot run WebRTC video in web pages loaded from the desktop.)
    -   In a browser on your development computer, load the browser_demo.html file (from the web server) Click the
        Connect and Publish buttons.
    -   Run the app on your Android device again.

## OpenTok Android SDK

* See [OpenTok Android SDK developer and client requirements](http://tokbox.com/opentok/libraries/client/android/#developerandclientrequirements) for a list or system requirements and supported devices.

* See the [OpenTok Android SDK Reference](http://tokbox.com/opentok/libraries/client/android/reference/index.html)
for details on the API.

* The OpenTok Android SDK is hosted on Maven. To use the SDK in your app, download it
  from `http://tokbox.bintray.com/maven`. For example:

  a) Edit the build.gradle for your project and add the following code snippet to the
     `allprojects/repositiories` section:

      maven { url  "http://tokbox.bintray.com/maven" }

  b) Modify build.gradle for your module and add the following code snippet to the `dependencies`
     section:

      compile 'com.opentok.android:opentok-android-sdk:2.9.+'

## Understanding the code

The MainActivity class defines the activity of the basic example.

The main_activity.xml defines a LinearLayout object used by the app.

The OpenTok Android SDK uses following permissions:

```java
    android.permission.CAMERA
    android.permission.INTERNET
    android.permission.RECORD_AUDIO
```

You do not need to add these to your app manifest. The OpenTok SDK adds them automatically.
However, if you use Android 21+, certain permissions require you to prompt the user.

### Adding views for videos

When the MainActivity activity is started, the onCreate() method sets the content view for
the activity

```java
    setContentView(R.layout.main_activity);
```

The app uses other view contains to displaying the publisher and subscriber videos:

```java
    publisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
    subscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
```

A *Publisher* is an object represents an audio-video stream sent from the Android device to
the OpenTok session. A *Subscriber* is an object that subscribes to an audio-video stream from
the OpenTok session that you display on your device. The subscriber stream can be one published
by your device or (more commonly) a stream another client publishes to the OpenTok session.

### Initializing a Session object and connecting to an OpenTok session

The code then calls a method to instantiate an a Session object and connection to the OpenTok session:

```java
    mSession = new Session(MainActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
    mSession.setSessionListener(this);
    mSession.connect(OpenTokConfig.TOKEN);
```

The Session constructor instantiates a new Session object.

- The first parameter of the method is the Android application context associated with this process.
- The second parameter is your OpenTok API key see the [Developer Dashboard](https://dashboard.tokbox.com/projects).
- The third parameter is the session ID for the OpenTok session your app connects to. You can generate a session ID from the [Developer Dashboard](https://dashboard.tokbox.com/projects) or from a
[server-side library](http://www.tokbox.com/opentok/docs/concepts/server_side_libraries.html).

The `setSessionListener()` method of the Session object sets up a listener for basic session-related
events:

```java
    mSession.setSessionListener(this);
```

Note that the main MainActivity class implements the Session.SessionListener interface.

The `connect()` method of the Session object connects your app to the OpenTok session:

```java
    mSession.connect(OpenTokConfig.TOKEN);
```

The `OpenTokConfig.TOKEN` constant is the token string for the client connecting to the session. See
[Token Creation Overview](http://tokbox.com/opentok/tutorials/create-token/) for details.
You can generate a token from the [Developer Dashboard](https://dashboard.tokbox.com/projects) or from an
[OpenTok server-side SDK](http://tokbox.com/opentok/libraries/server/). (In completed applications,
use the OpenTok server-side library to generate unique tokens for each user.)

When the app connects to the OpenTok session, the `onConnected()` method of the SessionListener
listener is called. An app must create a Session object and connect to the session it before the app
can publish or subscribe to streams in the session.

### Publishing an audio-video stream to a session

The `onConnected(Session session)` method is defined by the Session.SessionListener class. In the overridden
version of this method, the app instantiates a Publisher object by calling the Publisher constructor:

```java
    mPublisher = new Publisher(MainActivity.this, "publisher");
```

- The first parameter is the Android application context associated with this process.

- The second parameter is the name of the stream. This a string that appears at the bottom of the
  stream's view when the user taps the stream (or clicks it in a browser).

Next the code adds a Publisher.PublisherListener object to respond to publisher-related events:

```java
    mPublisher.setPublisherListener(this);
```

Note that the MainActivity class implements the Publisher.PublisherListener interface.

(The Publisher class extends the PublisherKit class. The PublisherKit class is a base class for
for streaming video to an OpenTok session. The Publisher class extends it, adding a default user
interface and video renderer, and capturing video from the Android device's camera.)

The `getView()` method of the Publisher object returns the view in which the Publisher will
display video, and this view is added to the publisherViewContainer:

```java
    publisherViewContainer.addView(mPublisher.getView());
```

Next, we call the `publish()` method of the Session object, passing in the Publisher object as a parameter:

```java
    mSession.publish(mPublisher);
```

This publishes a stream to the OpenTok session.

### Subscribing to streams

The `onStreamCreated()` method, defined by the PublisherKit.PublisherListener interface, is called
when the Publisher starts streaming:

```java
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");

        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }

        subscribeToStream(stream);
    }
```

When another client's stream is added to a session, the `onStreamReceived()` method of the
Session.SessionListener object is called:

```java
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber != null) {
            return;
        }

        subscribeToStream(stream);
    }
```

This app subscribes to one stream, at most. It either subscribes to the stream you publish,
or it subscribes to one of the other streams in the session (if there is one), based on the
`SUBSCRIBE_TO_SELF` property, which is set in the OpenTokConfig class.

Normally, an app would not subscribe to a stream it publishes. However, for this
test app, it is convenient for the client to subscribe to its own stream.)

The subscribeToStream() method initializes a Subscriber object for the stream:

```java
    mSubscriber = new Subscriber(MainActivity.this, stream);
```

- The first parameter is the Android application context associated with this process.

- The second parameter is the stream to subscribe to.

Next the code adds a Subscriber.VideoListener object to respond to publisher-related events:

```java
    mSubscriber.setVideoListener(this);
```

Note that the OpenTokHelloWorld class implements the Subscriber.VideoListener interface.

Then the code calls the `subscribe(SubscriberKit subscriber)` method of the Session object to subscribe to the stream

```java
   mSession.subscribe(mSubscriber);
```

When the subscriber's video stream is received, the `onVideoDataReceived(SubscriberKit subscriber)` method of the
Subscriber.VideoListener object is called:

```java
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mSubscriberViewContainer.addView(mSubscriber.getView());
    }
```

The method sets the video-scaling style for the subscriber so that the video scales to fill
the entire area of the renderer, with cropping as needed.:

```java
    subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL);
```

The app can also subscribe to streams published by other clients (rather than your own),
based on the `SUBSCRIBE_TO_SELF` setting. The `onStreamReceived(Session session, Stream stream)` method of the Session.Listener
interface is called when a new stream from another client enters the session:

```java
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber != null) {
            return;
        }

        subscribeToStream(stream);
    }
```

### Removing dropped streams

As streams published by other clients leave the session (when clients disconnect or stop
publishing), the `onStreamDropped(Session session, Stream stream)` method of the Session.SessionListener interface is called.
The app unsubscribes and removes a view for any Subscriber associated with the stream. The app also
subscribes to any other streams in the session:

```java
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber == null) {
            return;
        }

        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }
    }
```

### Knowing when you have disconnected from the session

When the app disconnects from the session, the `onDisconnected(Session session)` method of the
Session.SessionListener interface is called.

```java
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

        mSession = null;
    }
```

If an app cannot connect to the session (perhaps because of no network connection), the `onError()`
method of the Session.Listener interface is called:

```java
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }
```

## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
