opentok-androd-sdk-demo
=======================

This sample app is a basic sample app that shows the most basic features of the OpenTok 2.2
Android Beta SDK.

*Important:* Read "Testing the sample app" below for information on configuring and testing the sample app.

Also, be sure to read the README file for [the OpenTok 2.0 Android Beta SDK](../README.md).

Notes
-----

* The OpenTok Android 2.0 SDK is supported on the Samsung Galaxy S3.
* See the [API reference documentation](http://opentok.github.com/opentok-android-sdk) at the [OpenTok Android SDK project](https://github.com/opentok/opentok-android-sdk) on github.
* You cannot test using OpenTok videos in the ADT emulator.

Testing the sample app
----------------------

1. Import the project into ADT. (Select File > Import > Android > Existing Android Code into
   Workspace. Choose the samples/OpenTokHelloWorld directory. Then click Finish.)

   This project links to the opentok-android-sdk-2.2.jar file and the armeabi/libopentok.so file.
   Both of these libraries are required to develop apps that use the OpenTok 2.0 Android SDK.
   If these do not appear in the libs directory of your project, find them in the OpenTok/libs
   directory of the SDK and add them to the project.

   (From the desktop, drag the opentok-android-sdk-2.2.jar file and armeabi directory into the
   libs directory of the project in the ADT package explorer.)

2. Configure the project to use your own OpenTok session and token. If you don't have an OpenTok
   API key yet, [sign up for a Developer Account](https://dashboard.tokbox.com/signups/new).
   Then to generate the session ID and token, use the Project Tools on the
   [Project Details](https://dashboard.tokbox.com/projects) page.

   Open the OpenTokConfig.java file and set the `SESSION_ID`, `TOKEN`, and `API_KEY` strings
   to your own session ID, token, and API key respectively.

3.  Connect your Android device to a USB port on your computer. Set up
    [USB debugging](http://developer.android.com/tools/device.html) on your device.

4.  Run the app on your device, selecting the default activity as the launch action.

    The app should start on your connected device. The initial view of the app shows different
    processes you can run:

    * Hello World -- A simple example of publishing and subscribing to streams in a session
    * Hello World UI -- Adds custom UI controls to the Hello World app
    * Hello World Capturer -- Shows how to use a custom video capturer
    * Hello World Renderer -- Shows how to use a custom video renderer
    * Hello World Multiparty -- Shows how to created subclasses of the Session and Subscriber
      classes. It also shows how to use the signaling API.

5.  Tap the Hello World link in the main view of the app. This launches the Hello World activity
    in a new view.

    Once the app connects to the OpenTok session, it publishes an audio-video stream, which is
    displayed onscreen. Then, the same audio-video stream shows up as a subscribed stream
    (along with any other streams currently in the session).

6.  Close the app. Now set up the app to subscribe to audio-video streams other than your own:

    -   In the OpenTokConfig class (in the com.opentok.android.demo.config package), change the
        `SUBSCRIBE_TO_SELF` property to be set to `false`.
    -   Edit browser_demo.html located in the root directory of this project, and modify the variables `apiKey`, `sessionId`,
        and `token` with your OpenTok API Key, and with the matching session ID and token. (Note that you would normally use
        the OpenTok server-side libraries to issue unique tokens to each client in a session. But for testing purposes,
        you can use the same token on both clients. Also, depending on your app, you may use the OpenTok server-side
        libraries to generate new sessions.)
    -   Add the browser_demo.html file to a web server. (You cannot run WebRTC video in web pages loaded from the desktop.)
    -   In a browser on your development computer, load the browser_demo.html file (from the web server) Click the
        Connect and Publish buttons.
    -   Run the app on your Android device again.

In addition to the Hello World activity, try running the other activities from the main menu of
the app:

* Hello World UI -- Adds custom UI controls to the Hello World app.
* Hello World Capturer -- Shows how to use a custom video capturer.
* Hello World Renderer -- Shows how to use a custom video renderer.
* Hello World Subclassing -- Shows how to created subclasses of the Session and Subscriber
  classes. It also shows how to use the signaling API.

For information on how these activities use the OpenTok Android SDK, see the next section,
"Understanding the code."

Understanding the code
----------------------

The OpenTokHelloWorld class defines the Hello World activity.

The MainActivity.xml defines a LinearLayout object used by the app.

The AndroidManifest.xml app includes required permissions and features used by an OpenTok app:

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />

### Adding views for videos

When the OpenTokHelloWorld activity is started, the onCreate() method sets the content view for
the activity

    setContentView(R.layout.main_layout);

The app uses other view contains to displaying the publisher and subscriber videos:

    publisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
    subscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);

A *Publisher* is an object represents an audio-video stream sent from the Android device to
the OpenTok session. A *Subscriber* is an object that subscribes to an audio-video stream from
the OpenTok session that you display on your device. The subscriber stream can be one published
by your device or (more commonly) a stream another client publishes to the OpenTok session.

The `onCreate()` method also instantiates an ArrayList for keeping references to OpenTok Stream objects:

    mStreams = new ArrayList<Stream>();

A *Stream* object represents an audio-video stream in the OpenTok session. This app subscribes to only one stream at a time,
and it uses this ArrayList to subscribe to a new stream if the a subscribed stream drops from the session.

### Initializing a Session object and connecting to an OpenTok session

The code then calls a method to instantiate an a Session object and connection to the OpenTok session:

    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(OpenTokHelloWorld.this,
                    OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        }
    }

The Session constructor instantiates a new Session object.

- The first parameter of the method is the Android application context associated with this process.
- The second parameter is your OpenTok API key see the [Developer Dashboard](https://dashboard.tokbox.com/projects).
- The third parameter is the session ID for the OpenTok session your app connects to. You can generate a session ID from the [Developer Dashboard](https://dashboard.tokbox.com/projects) or from a
[server-side library](http://www.tokbox.com/opentok/docs/concepts/server_side_libraries.html).

The `setSessionListener()` method of the Session object sets up a listener for basic session-related
events:

    mSession.setSessionListener(this);

Note that the main HelloWorldActivity class implements the Session.SessionListener interface.

The `connect()` method of the Session object connects your app to the OpenTok session:

    mSession.connect(OpenTokConfig.TOKEN);

The `OpenTokConfig.TOKEN` constant is the token string for the client connecting to the session. See 
[Token Creation Overview](http://tokbox.com/opentok/tutorials/create-token/) for details.
You can generate a token from the [Developer Dashboard](https://dashboard.tokbox.com/projects) or from an
[OpenTok server-side SDK](http://tokbox.com/opentok/libraries/server/). (In completed applications,
use the OpenTok server-side library to generate unique tokens for each user.)

When the app connects to the OpenTok session, the `onConnected()` method of the SessionListener
listener is called. An app must create a Session object and connect to the session it before the app
can publish or subscribe to streams in the session.


### Publishing an audio-video stream to a session

The `onSessionConnected()` method is defined by the Session.Listener class. In the overridden
version of this method, the app instantiates a Publisher object by calling the Publisher constructor:

    mPublisher = new Publisher(OpenTokHelloWorld.this,
                            OpenTokHelloWorld.this, "publisher");

- The first parameter is the Android application context associated with this process.

- The second parameter is the name of the stream. This a string that appears at the bottom of the 
  stream's view when the user taps the stream (or clicks it in a browser).

Next the code adds a Publisher.PublisherListener object to respond to publisher-related events:

    mPublisher.setPublisherListener(this);
  
Note that the OpenTokHelloWorld class implements the Publisher.PublisherListener interface.

(The Publisher class extends the PublisherKit class. The PublisherKit class is a base class for
for streaming video to an OpenTok session. The Publisher class extends it, adding a default user
interface and video renderer, and capturing video from the Android device's camera. For more
information on the PublisherKit class, see "Using a custom video capturer" and "Using a custom
video renderer" below.)

The `getView()` method of the Publisher object returns the view in which the Publisher will
display video, and this view is added to the publisherViewContainer:

    publisherViewContainer.addView(mPublisher.getView(), layoutParams);

Next, we call the `publish()` method of the Session object, passing in the Publisher object as a parameter:

    mSession.publish(mPublisher);

This publishes a stream to the OpenTok session.

### Subscribing to streams

The `onStreamCreated()` method, defined by the PublisherKit.PublisherListener interface, is called
when the Publisher starts streaming:

    public void onStreamCreated(PublisherKit publisher, Stream stream) {
        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

When another client's stream is added to a session, the `onStreamReceived()` method of the 
Session.SessionListener object is called:

    public void onStreamReceived(Session session, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

This app subscribes to one stream, at most. It either subscribes to the stream you publish,
or it subscribes to one of the other streams in the session (if there is one), based on the
`SUBSCRIBE_TO_SELF` property, which is set in the OpenTokConfig class.

Normally, an app would not subscribe to a stream it publishes. (See the last step of "Testing the
sample app" above.) However, for this test app, it is convenient for the client to subscribe to its
own stream.)

The subscribeToStream() method initializes a Subscriber object for the stream:

    mSubscriber = new Subscriber(OpenTokHelloWorld.this, stream);

- The first parameter is the Android application context associated with this process.

- The second parameter is the stream to subscribe to.

Next the code adds a Subscriber.VideoListener object to respond to publisher-related events:

    mSubscriber.setVideoListener(this);
  
Note that the OpenTokHelloWorld class implements the Subscriber.VideoListener interface.

Then the code calls the `subscribe()` method of the Session object to subscribe to the stream

   mSession.subscribe(mSubscriber);

When the subscriber's video stream is received, the `onVideoDataReceived()` method of the
Subscriber.VideoListener object is called:

    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOGTAG, "First frame received");

        // stop loading spinning
        mLoadingSub.setVisibility(View.GONE);
        attachSubscriberView(mSubscriber);
    }

This method in turn calls the `attachSubscriberView()` method, which adds the subscriber's view
(which contains the video) to the app:

    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
            getResources().getDisplayMetrics().widthPixels, getResources()
                    .getDisplayMetrics().heightPixels);
    subscriberViewContainer.addView(mSubscriber.getView(), layoutParams);

The method also sets the video-scaling style for the subscriber so that the video scales to fill
the entire area of the renderer, with cropping as needed.:

    subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
            BaseVideoRenderer.STYLE_VIDEO_FILL);

The app can also subscribe to streams published by other clients (rather than your own),
based on the `SUBSCRIBE_TO_SELF` setting. The `onStreamReceived()` method of the Session.Listener
interface is called when a new stream from another client enters the session:

    public void onStreamReceived(Session session, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

### Removing dropped streams

As streams published by other clients leave the session (when clients disconnect or stop
publishing), the `onStreamDropped()` method of the Session.SessionListener interface is called.
The app unsubscribes and removes a view for any Subscriber associated with the stream. The app also
subscribes to any other streams in the session:

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        if (mSubscriber != null) {
            unsubscribeFromStream(stream);
        }
    }
    
    private void unsubscribeFromStream(Stream stream) {
        mStreams.remove(stream);
        if (mSubscriber.getStream().getStreamId().equals(stream.getStreamId())) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber = null;
            if (!mStreams.isEmpty()) {
                subscribeToStream(mStreams.get(0));
            }
        }
    }

### Knowing when you have disconnected from the session

When the app disconnects from the session, the `onDisconnected()` method of the
Session.SessionListener interface is called. In this method, the app removes views for any Publisher
or Subscriber:

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOGTAG, "Disconnected from the session.");
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
        }

        mPublisher = null;
        mSubscriber = null;
        mStreams.clear();
        mSession = null;
    }

If an app cannot connect to the session (perhaps because of no network connection), the `onError()`
method of the Session.Listener interface is called:

   @Override
   public void onError(Session session, OpentokError exception) {
       Log.i(LOGTAG, "Session exception: " + exception.getMessage());
   }

### Adding user interface controls

The UIActivity class shows how you can add user interface controls for the following:

* Muting and resuming a subscriber's audio
* Turning a publisher's audio stream on and off
* Swapping the publisher's camera

The user interface is defined in the com.opentok.android.demo.ui package. The UIActivity class
implements the following interfaces defined in that package:

* SubscriberControlFragment.SubscriberCallbacks
* PublisherControlFragment.PublisherCallbacks

When the user taps the mute button for a subscriber, the following method of the OpenTokUI class
is invoked:

    @Override
    public void onMuteSubscriber() {
        if (mSubscriber != null) {
            mSubscriber.setSubscribeToAudio(!mSubscriber.getSubscribeToAudio());
        }
    }

The `setSubscribeToAudio()` method of a Subscriber object toggles its audio on or off, based on a
Boolean parameter. The `getSubscribeToAudio()` method of the Subscriber returns true if the the
Subscriber is subscribed to the audio track, and it returns false if it is not.

When the user taps the mute button for the Publisher, the following method of the OpenTokUI class
is invoked:

    @Override
    public void onMutePublisher() {
        if (mPublisher != null) {
            mPublisher.setPublishAudio(!mPublisher.getPublishAudio());
        }
    }

The `setPublishAudio()` method of a Publisher object toggles its audio on or off, based on a
Boolean parameter. The getPublishAudio() method of the Subscriber Publisher true if the the
Publisher is publishing an audio track, and it returns false if it is not.

When the user taps the swapCamera button, the following method of the OpenTokUI class
is invoked:

    @Override
    public void onSwapCamera() {
        if (mPublisher != null) {
            mPublisher.swapCamera();
        }
    }

The `swapCamera()` method of a Publisher object changes the camera used to the next available camera
on the device (if there is one).

### Using a custom video capturer

The VideoCapturerActivity class shows how you can use a custom video capturer for a publisher. After
instantiating a Publisher object, the code sets a custom video capturer by calling the
`setCapturer()` method of the Publisher:

    mPublisher = new Publisher(VideoCapturerActivity.this,
            "publisher");
    mPublisher.setPublisherListener(this);
    // use an external customer video capturer
    mPublisher.setCapturer(new CustomVideoCapturer(
            VideoCapturerActivity.this));

The CustomVideoCapturer class is defined in the com.opentok.android.demo.video package.
This class extends the BaseVideoCapturer class, defined in the OpenTok Android SDK.
The `getCaptureSettings()` method returns the settings of the video capturer, including the frame
rate, width, height, video delay, and video format for the capturer:

    @Override
    public CaptureSettings getCaptureSettings() {
        // Set the preferred capturing size
        configureCaptureSize(640, 480);

        CaptureSettings settings = new CaptureSettings();
        settings.fps = mCaptureFPS;
        settings.width = mCaptureWidth;
        settings.height = mCaptureHeight;
        settings.format = NV21;
        settings.expectedDelay = 0;
        return settings;
    }

The app calls `startCapture()` to start capturing video from the custom video capturer.

The class also implements the android.hardware.Camera.PreviewCallback interface. The
onPreviewFrame() method of this interface is called as preview frames of the camera become
available. In this method, the app calls the provideByteArrayFrame() method of the
CustomVideoCapturer class (inherited from the BaseVideoCapturer class). This method
provides a video frame, defined as a byte array, to the video capturer:

    provideByteArrayFrame(data, NV21, mCaptureWidth,
            mCaptureHeight, currentRotation, isFrontCamera());

The publisher adds this video frame to the published stream.

### Using a custom video renderer

The VideoRendererActivity class shows how you can use a custom video renderer for publisher and
subscriber videos.

After instantiating a Publisher object, the code sets a custom video capturer by calling the setCapturer() method of the Publisher:

   mPublisher = new Publisher(OpenTokVideoRenderer.this, "publisher");
   mPublisher.setPublisherListener(this);
   // use an external custom video renderer
   mPublisher.setRenderer(new CustomVideoRenderer(this));
   
The CustomVideoRenderer class is defined in the com.opentok.android.demo.video package.
This class extends the BaseVideoRenderer class, defined in the OpenTok Android SDK.
The CustomVideoRenderer class includes a MyRenderer subclass that implements GLSurfaceView.Renderer.
This class includes a `displayFrame()` method that renders a frame of video to an Android view.

The CustomVideoRenderer constructor sets a property to an instance of the MyRenderer class.

    mRenderer = new MyRenderer();

The `onFrame()` method of the CustomVideo renderer is inherited from the BaseVideoRenderer class.
This method is called at the specified frame rate. It then calls the `displayFrame()` method of
the MyVideoRenderer instance:

    public void onFrame(Frame frame) {
        mRenderer.displayFrame(frame);
        mView.requestRender();
    }

### Adding subclasses of the OpenTok Android SDK classes

The MultipartyActive class instantiates MySession and MySubscriber classes, which are both defined
in the com.opentok.android.demo.multiparty package. The MySession class is a subclass of the Session
class, defined in the OpenTok Android SDK in the com.opentok.android package. The MySubscriber class
is a subclass of the Subscriber class, defined in the com.opentok.android package.

The PublisherKit, Publisher, Session, and SubscriberKit classes include protected callback
methods for events. To process events, you can extend these classes and override these
methods instead of overriding methods of the Listener interfaces. For example, in the MySession
class extends the com.opentok.android.Session class. And it overrides the
`onConnected()` method, using it as a callback for when the client connects to the
OpenTok session:

    @Override
    protected void onConnected() {
        Publisher p = new Publisher(mContext, "MyPublisher");
        publish(p);

        // Add video preview
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mPreview.addView(p.getView(), lp);
        p.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        presentText("Welcome to OpenTok Chat.");
    }


### Sending and receiving messages in the session

The MultipartyActive class instantiates MySession class, which is a subclass of the Session
class, defined in the OpenTok Android SDK. It includes method that use the signaling API.

The `sendChatMessage()` method is called when the user enters a text message in the app (and taps
the Enter button):

    public void sendChatMessage(String message) {
        sendSignal("chat", message);
        presentMessage("Me", message);
    }
    
The `sendSignal()` method is defined in the Session class. It sends a signal (defined by a type
string and a data string) to all clients connected in the session.

(The presentMessage() method simply displays the message on the onscreen view.)

The MySession class also implements the Session.SignalListener interface. This interface includes an
`onSignalReceived()` method, which is called when the client receives a signal in the session. This
includes signals sent by the local Android client. The `onSignal()` method only displays signals
sent from other clients (since the `sendChatMessage()` method has already displayed messages send by
the local client.) The `getConnection()` method of the `onSignalReceived()` method indicates the
client that sent the signal. Compare this to the value returned by the `getConnection()` method
(inherited from the Session class) to determine if the signal was sent by the local client:

    @Override
    protected void onSignalReceived(String type, String data,
            Connection connection) {

        String mycid = this.getConnection().getConnectionId();
        String cid = connection.getConnectionId();
        if (!cid.equals(mycid)) {
            if ("chat".equals(type)) {
                Player p = mPlayerConnection.get(cid);
                if (p != null) {
                    presentMessage(p.getName(), data);
                }
            }
        }
    }

To see the signaling API in action:

1. Open the app on an Android device. Then tap Hello World Subclassing.

2. Open the browser_demo.html file (in the samples directory) in a text editor, and make sure it
   uses the same API key and session ID as the Android app. Also, make sure that it uses a valid
   OpenTok token for the session.

3. Open the browser_demo.html file on a web server.

4. Click the Connect button on the web page. Then click the Publish button. Then click the
   Signal button. Signals sent from the browser page are sent to the Android app (and to any other
   clients connected to the session).

### Responding to archive-related events

[OpenTok archiving](http://tokbox.com/opentok/tutorials/archiving) lets you record, save, and
retrieve OpenTok sessions. You start and stop recording an archive of an OpenTok session
using the OpenTok server SDKs or the OpenTok REST API.

The UIActivity class in the sample code implements the Session.ArchiveListener interface. When
the app creates the Session (in the `UIActivity.sessionConnect()` method), the code sets the
UIActivity object as the archive event listener:

    mSession.setArchiveListener(this);

When an archive of a session starts (or when you connect to a session for which archive
recording has already started), the `onArchiveStarted(session, id, name)` method of the
Session.ArchiveListener is called. When archiving starts, your app should display some user
interface element that notifies the user of the recording. The sample app includes a
PublisherStatusFragment class that displays status notifications. The `onArchiveStarted()` method
calls the `updateArchivingUI()` method of the PublisherStatusFragment object (passing in `true`).
This adds a UI notification to the PublisherStatusFragment view:

    @Override
    public void onArchiveStarted(Session session, String id, String name) {
        Log.i(LOGTAG, "Archiving starts");
        mPublisherFragment.showPublisherWidget(false);
    
        archiving = true;
        mPublisherStatusFragment.updateArchivingUI(true);
        mPublisherFragment.showPublisherWidget(true);
        mPublisherFragment.initPublisherUI();
        setPubViewMargins();
    
        if (mSubscriber != null) {
            mSubscriberFragment.showSubscriberWidget(true);
        }
    }

Similarly, when an archive of a session stops, the `onArchiveStopped(session, id)` method of the Session.ArchiveListener is called. The `onArchiveStopped()` method calls the `updateArchivingUI()` method of the PublisherStatusFragment object (this time, passing in `false`). This updates the UI
notification to the PublisherStatusFragment view:

    @Override
    public void onArchiveStopped(Session session, String id) {
        Log.i(LOGTAG, "Archiving stops");
        archiving = false;
    
        mPublisherStatusFragment.updateArchivingUI(false);
        setPubViewMargins();
    }

Next steps
----------

For details on the full OpenTok Android API, see the [reference documentation](../docs/index.html).
