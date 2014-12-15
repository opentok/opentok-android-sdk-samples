OpenTok Android SDK Samples
===========================

This is a basic sample app that shows the most basic features of the [OpenTok Android SDK](http://tokbox.com/opentok/libraries/client/android/).

*Important:* Read "Testing the sample app" below for information on configuring and testing the sample app.

Notes
-----

* See [OpenTok Android SDK developer and client requirements](http://tokbox.com/opentok/libraries/client/android/#developerandclientrequirements) for a list or system requirements and supported devices.

* See the [OpenTok Android SDK Reference](http://tokbox.com/opentok/libraries/client/android/reference/index.html)
for details on the API.
 
* You cannot publish streams in the Android Emulator. Build and deploy to a supported device.

Testing the sample app
----------------------

1. Import the project into Android Studio or ADT.

   This project links to the opentok-android-sdk-2.4.0.jar file and the armeabi/libopentok.so or
   x86/libopentok.so file. These libraries are required to develop apps that use the OpenTok
   Android SDK. These are included in the OpenTok/libs subdirectory of the SDK, available at
   <http://tokbox.com/opentok/libraries/client/android/>.

   In Android Studio, copy the opentok-android-sdk-2.4.0.jar file into the libs directory, and
   copy the armeabi and x86 directories into a app/src/main/jniLibs (which you may need to create).

   If you are using ADT, from the desktop, drag the opentok-android-sdk-2.4.0.jar file and armeabi
   or x86 directory into the libs directory of your project in the ADT package explorer.

2. Configure the project to use your own OpenTok session and token. If you don't have an OpenTok
   API key yet, [sign up for a Developer Account](https://dashboard.tokbox.com/signups/new).
   Then to generate a test session ID and token, use the Project Tools on the
   [Project Details](https://dashboard.tokbox.com/projects) page.

   Open the OpenTokConfig.java file and set the `SESSION_ID`, `TOKEN`, and `API_KEY` strings
   to your own session ID, token, and API key respectively.

   For more information, see the OpenTok [Session Creation
   Overview](https://tokbox.com/opentok/tutorials/create-session/) and the [Token Creation
   Overview](https://tokbox.com/opentok/tutorials/create-token/).

3.  Connect your Android device to a USB port on your computer. Set up
    [USB debugging](http://developer.android.com/tools/device.html) on your device.

4.  Run the app on your device, selecting the default activity as the launch action.

    The app should start on your connected device. The initial view of the app shows different
    processes you can run:
    * Hello World -- A simple example of publishing and subscribing to streams in a session
    * UI Controls-- Adds custom UI controls to the Hello World app
    * Custom Capturer -- Shows how to use a custom video capturer
    * Custom Renderer -- Shows how to use a custom video renderer
    * Multiparty -- Shows how to created subclasses of the Session and Subscriber
      classes. It also shows how to use the signaling API.
    * Voice Only -- Shows how to implement a voice-only OpenTok session.
    * Audio device -- Shows how to use the audio driver API to implement a custom audio
      capturer and player.
    * Emulator Hello World -- Shows how to correct the video orientation when testing in a
      virtual machine.
    * Screen Sharing -- Shows how to publish a screen-sharing stream to a session.

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
the app.

For information on how these activities use the OpenTok Android SDK, see the next section,
"Understanding the code."

Understanding the code
----------------------

The HelloWorldActivity class defines the activity of the basic example.

The main_activity.xml defines a LinearLayout object used by the app.

The AndroidManifest.xml app includes required permissions and features used by an OpenTok app:

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />

### Adding views for videos

When the HelloWorldActivity activity is started, the onCreate() method sets the content view for
the activity

    setContentView(R.layout.main_activity);

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
            mSession = new Session(HelloWorldActivity.this,
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

The `onConnected(Session session)` method is defined by the Session.SessionListener class. In the overridden
version of this method, the app instantiates a Publisher object by calling the Publisher constructor:

    mPublisher = new Publisher(HelloWorldActivity.this, "publisher");

- The first parameter is the Android application context associated with this process.

- The second parameter is the name of the stream. This a string that appears at the bottom of the 
  stream's view when the user taps the stream (or clicks it in a browser).

Next the code adds a Publisher.PublisherListener object to respond to publisher-related events:

    mPublisher.setPublisherListener(this);
  
Note that the HelloWorldActivity class implements the Publisher.PublisherListener interface.

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

Then the code calls the `subscribe(SubscriberKit subscriber)` method of the Session object to subscribe to the stream

   mSession.subscribe(mSubscriber);

When the subscriber's video stream is received, the `onVideoDataReceived(SubscriberKit subscriber)` method of the
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
based on the `SUBSCRIBE_TO_SELF` setting. The `onStreamReceived(Session session, Stream stream)` method of the Session.Listener
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
publishing), the `onStreamDropped(Session session, Stream stream)` method of the Session.SessionListener interface is called.
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

When the app disconnects from the session, the `onDisconnected(Session session)` method of the
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

The `setSubscribeToAudio(boolean subscribeToAudio)` method of a Subscriber object toggles its audio on or off, based on a
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

The `setPublishAudio(boolean publishAudio)` method of a Publisher object toggles its audio on or off, based on a
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
`setCapturer(BaseVideoCapturer capturer)` method of the Publisher:

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

After instantiating a Publisher object, the code sets a custom video renderer by calling the 'setRenderer(BaseVideoRenderer renderer)' method of the Publisher:

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

### Using a custom audio driver

The AudioDeviceActivity sample shows how you can use a custom audio driver for publisher and
subscriber audio.

The AudioDeviceActivity class instantiates a CustomAudioDevice instance and passes it into the
`AudioDeviceManager.setAudioDevice()` method:

    CustomAudioDevice customAudioDevice = new CustomAudioDevice(
        AudioDeviceActivity.this);
    AudioDeviceManager.setAudioDevice(customAudioDevice);
    mSession = new Session(AudioDeviceActivity.this,
        OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);

The CustomAudioDevice class extends the BaseAudioDevice class, defined in the OpenTok Android SDK. This class includes methods for setting up and using a custom audio driver. The audio driver
includes an audio capturer -- used to get audio samples from a audio source -- and an audio
renderer -- used to play back audio samples from the OpenTok streams the client has subscribed to.

Note that you must call the method `AudioDeviceManager.setAudioDevice()` before you instantiate
a Session object (and connect to the session).

The constructor for the CustomAudioDevice class instantiates two instances of the
BaseAudioDevice.AudioSettings class, defined in the OpenTok Android SDK. These are settings for
audio capturing and audio rendering:

    m_captureSettings = new AudioSettings(SAMPLING_RATE,
            NUM_CHANNELS_CAPTURING);
    m_rendererSettings = new AudioSettings(SAMPLING_RATE,
            NUM_CHANNELS_RENDERING);

The CustomAudioDevice class overrides the `initCapturer()` method, defined in the BaseAudioDevice
class. This method initializes the app's audio capturer, instantiating a an
andriod.media.AudioRecord instance to be used to capture audio from the device's audio input
hardware:

    m_audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
        m_captureSettings.getSampleRate(),
        NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO
                : AudioFormat.CHANNEL_IN_STEREO,
        AudioFormat.ENCODING_PCM_16BIT, recBufSize);

The `initCapturer()` method also sets up a thread to capture audio from the device:

    new Thread(m_captureThread).start();

The CustomAudioDevice overrides the `startCapturer()` method, which is called when the app starts
sampling audio to be sent to the publisher's stream. The audio capture thread reads audio samples
from the AudioRecord object into a buffer, `m_recbuffer`:

    int lengthInBytes = (samplesToRec << 1)
            * NUM_CHANNELS_CAPTURING;
    int readBytes = m_audioRecord.read(m_tempBufRec, 0,
            lengthInBytes);

    m_recBuffer.rewind();
    m_recBuffer.put(m_tempBufRec);

    samplesRead = (readBytes >> 1) / NUM_CHANNELS_CAPTURING;

The `getAudioBus()` method, defined in the BaseAudioDevice class, returns a BaseAudioDevice.AudioBus
object, also defined in the OpenTok Android SDK. This audio bus object includes a
`writeCaptureData()` method, which you call to send audio samples to be used as audio data for the
publisher's stream:

    getAudioBus().writeCaptureData(m_recBuffer, samplesRead);

The CustomAudioDevice class overrides the `initRenderer()` method, defined in the BaseAudioDevice
class. This method initializes the app's audio renderer, instantiating an andriod.media.AudioTrack
instance. This object will be used to play back audio to the device's audio output hardware:

    m_audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
            m_rendererSettings.getSampleRate(),
            NUM_CHANNELS_RENDERING == 1 ? AudioFormat.CHANNEL_OUT_MONO
                    : AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT, playBufSize,
            AudioTrack.MODE_STREAM);

The `initRenderer()` method also sets up a thread to play back audio to the device's audio output
hardware:

    new Thread(m_renderThread).start();

The CustomAudioDevice overrides the `startRenderer()` method, which is called when the app starts
receiving audio from subscribed streams.

The AudioBus object includes a `readRenderData()` method, which the audio render thread calls
to read audio samples from the subscribed streams into a playback buffer:

    int samplesRead = getAudioBus().readRenderData(
            m_playBuffer, samplesToPlay);

Sample data is written from the playback buffer to the audio track:

    int bytesRead = (samplesRead << 1)
            * NUM_CHANNELS_RENDERING;
    m_playBuffer.get(m_tempBufPlay, 0, bytesRead);

    int bytesWritten = m_audioTrack.write(m_tempBufPlay, 0,
            bytesRead);

    // increase by number of written samples
    m_bufferedPlaySamples += (bytesWritten >> 1)
            / NUM_CHANNELS_RENDERING;

    // decrease by number of played samples
    int pos = m_audioTrack.getPlaybackHeadPosition();
    if (pos < m_playPosition) {
        // wrap or reset by driver
        m_playPosition = 0;
    }
    m_bufferedPlaySamples -= (pos - m_playPosition);
    m_playPosition = pos;

### Adding subclasses of the OpenTok Android SDK classes

The MultipartyActivity class instantiates MySession and MySubscriber classes, which are both defined
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

### Displaying an audio-level meter

The VoiceOnlyActivity class adds an AudioLevelListener instance for the
Subscriber:

    subscriber
        .setAudioLevelListener(new SubscriberKit.AudioLevelListener() {
            @Override
            public void onAudioLevelUpdated(
                    SubscriberKit subscriber, float audioLevel) {
                meterView.setMeterValue(audioLevel);
            }
        });

This method is called periodically with updates to the Subscriber's audio level.
The method updates the `meterView` element based on the audio level.

Similarly, the VoiceOnlyActivity class adds an AudioLevelListener instance for
the Publisher, which works similarly:

    mPublisher.setAudioLevelListener(new PublisherKit.AudioLevelListener() {
        @Override
        public void onAudioLevelUpdated(PublisherKit publisher,
                float audioLevel) {
            meterView.setMeterValue(audioLevel);
        }
    });

### Sending and receiving messages in the session

The MultipartyActivity class instantiates MySession class, which is a subclass of the Session
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

1. Open the app on an Android device. Then tap Multiparty.

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

Similarly, when an archive of a session stops, the `onArchiveStopped(session, id)` method of the Session.ArchiveListener is called. The `onArchiveStopped(Session session, String id)` method calls the `updateArchivingUI()` method of the PublisherStatusFragment object (this time, passing in `false`). This updates the UI
notification to the PublisherStatusFragment view:

    @Override
    public void onArchiveStopped(Session session, String id) {
        Log.i(LOGTAG, "Archiving stops");
        archiving = false;
    
        mPublisherStatusFragment.updateArchivingUI(false);
        setPubViewMargins();
    }

### Testing in the Android Emulator

You can use the OpenTok Android SDK in a virtual machine in the Android Emulator. Note that you can
set the front and back cameras to use either the computer's webcam or an emulator camera. However,
the orientation of the video from the camera can be rotated incorrectly. The Emulator Hello World
activity corrects this issue.

Upon connecting to the OpenTok session, the app instantiates a Publisher object, and calls its
`setCapturer()` method to use a custom video capturer, defined by the CustomEmulatorVideoCapturer
class:

    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "Connected to the session.");
        if (mPublisher == null) {
            mPublisher = new Publisher(EmulatorActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            // use an external customer video capturer for emulator
            mPublisher.setCapturer(new CustomEmulatorVideoCapturer(EmulatorActivity.this));
            attachPublisherView(mPublisher);
            mSession.publish(mPublisher);
        }
    }

The CustomEmulatorVideoCapturer (defined in the com.opentok.android.demo.video package) defines a
custom video capturer (see "Using a custom video capturer"). The `onPreviewFrame(byte[] data,
Camera camera)` method is called when the video capturer supplies a frame of video. The
`compensateCameraRotation()` method adjusts the orientation of the video stream based on the
orientation of the virtual device:

    private int compensateCameraRotation(int uiRotation) {

        int cameraRotation = 0;
        switch (uiRotation) {
        case (Surface.ROTATION_0):
            cameraRotation = 0;
            break;
        case (Surface.ROTATION_90):
            cameraRotation = 270;
            break;
        case (Surface.ROTATION_180):
            cameraRotation = 180;
            break;
        case (Surface.ROTATION_270):
            cameraRotation = 90;
            break;
        default:
            break;
        }

        int cameraOrientation = this.getNaturalCameraOrientation();

        int totalCameraRotation = 0;
        boolean usingFrontCamera = this.isFrontCamera();
        if (usingFrontCamera) {
            // The front camera rotates in the opposite direction of the
            // device.
            int inverseCameraRotation = (360 - cameraRotation) % 360;
            totalCameraRotation = (inverseCameraRotation + cameraOrientation) % 360;
        } else {
            totalCameraRotation = (cameraRotation + cameraOrientation) % 360;
        }

        return totalCameraRotation;
    }

### Screen sharing

You can use a custom video capturer to use a view from the Android application as the source of
a published stream. (See "Using a custom video capturer" for basic information on using a custom
video capturer.)

When the app starts up, the `onCreate(Bundle savedInstanceState)` method instantiates a WebView
object:

    //We are using a webView to show the screensharing action
    //If we want to share our screen we could use: mView = ((Activity)this.context).getWindow().getDecorView().findViewById(android.R.id.content);
    mPubScreenWebView = (WebView) findViewById(R.id.webview_screen);

The app will use this WebView as the source for the publisher video (instead of a camera).

Upon connecting to the OpenTok session, the app instantiates a Publisher object, and calls its
`setCapturer()` method to use a custom video capturer, defined by the ScreensharingCapturer
class:

    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "Connected to the session.");
    
        //Start screensharing
        if (mPublisher == null) {
            mPublisher = new Publisher(ScreenSharingActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            mPublisher
                    .setPublisherVideoType(PublisherKitVideoType.PublisherKitVideoTypeScreen);
            ScreensharingCapturer screenCapturer = new ScreensharingCapturer(
                    this, mPubScreenWebView);
            mPublisher.setCapturer(screenCapturer);
            loadScreenWebView();
        
            mSession.publish(mPublisher);
        }
    }

The `onConnected(Session session)` method also calls the `loadScreenWebView()` method. This method
configures the WebView object, loading the Google URL:

    private void loadScreenWebView(){
            mPubScreenWebView.setWebViewClient(new WebViewClient());
            WebSettings webSettings = mPubScreenWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mPubScreenWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
              // to turn off hardware-accelerated canvas
            mPubScreenWebView.loadUrl("http://www.google.com");
        }

Note that the `mPubScreenWebView` object is passed into the ScreensharingCapturer() constructor,
which assigns it to the `contentView` property. The `newFrame()` method is called when the video
capturer supplies a new frame to the video stream. It creates a canvas, draws the `contentView`
to the canvas, and assigns the bitmap representation of `contentView` to the frame to be sent:

    Runnable newFrame = new Runnable() {
        @Override
        public void run() {
            if (capturing) {
                int width = contentView.getWidth();
                int height = contentView.getHeight();
                
                if (frame == null ||
                    ScreensharingCapturer.this.width != width ||
                    ScreensharingCapturer.this.height != height) {
                    
                    ScreensharingCapturer.this.width = width;
                    ScreensharingCapturer.this.height = height;
                    
                    if (bmp != null) {
                        bmp.recycle();
                        bmp = null;
                    }
                    
                    bmp = Bitmap.createBitmap(width,
                            height, Bitmap.Config.ARGB_8888);
                    
                    canvas = new Canvas(bmp);
                    frame = new int[width * height];
                }
                
                contentView.draw(canvas);
                
                bmp.getPixels(frame, 0, width, 0, 0, width, height);
 
                provideIntArrayFrame(frame, ARGB, width, height, 0, false);
 
                mHandler.postDelayed(newFrame, 1000 / fps);
            }
        }
    };

Next steps
----------

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).