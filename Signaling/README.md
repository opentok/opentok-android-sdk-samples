Learning OpenTok Android Sample App
===================================

This sample app shows how to accomplish basic tasks using the [OpenTok Android SDK] [1].
It connects the user with another client so that they can share an OpenTok audio-video
chat session. The app uses the OpenTok Android SDK to implement the following:

* Connect to an OpenTok session
* Publish an audio-video stream to the session
* Subscribe to another client's audio-video stream
* Record the session, stop the recording, and view the recording
* Implement text chat
* A simple custom audio driver for audio input and output
* A custom video renderer
* A simple custom video capturer
* A custom video capturer that uses the device camera
* Publishing a screen-sharing stream
* A video capturer that lets you obtain still screen captures of the camera used by a publisher

The code for this sample is found the following git branches:

* *basics.step-1* -- This branch shows you how to set up your project to use the OpenTok Android SDK.

* *basics.step-4* -- This branch shows you how to connect to the OpenTok session.

* *basics.step-5* -- This branch shows you how publish a stream to the OpenTok session.

* *basics.step-6* -- This branch shows you how to subscribe to a stream on the OpenTok session.

* *archiving.step-1* -- This branch shows you how to record the session.

* *signaling.step-1* -- This branch shows you how to use the OpenTok signaling API.

* *signaling.step-2* -- This branch shows you how to implement text chat using the OpenTok
signaling API.

* *signaling.step-3* -- This branch adds some UI improvements for the text chat feature.

* *audio-driver.step-1* -- This branch shows you how to implement a custom audio driver that
  uses a simple audio capturer.

* *audio-driver.step-2* -- This branch shows you how to implement a custom audio driver that
  uses a simple audio renderer.

* *basic-renderer* -- This branch shows the basics of implementing a custom video renderer
  for an OpenTok subscriber.

* *basic-capturer* -- This branch shows the basics of implementing a custom video capturer
  for an OpenTok publisher.

* *camera-capturer* -- This branch shows you how to use a custom video capturer using
  the device camera as the video source.

* *screensharing* -- This branch shows you how to use the device's screen (instead of a
  camera) as the video source for a published stream.


## basics.step-1: Starting Point

The basics.step-1 branch includes a basic Android application. Complete the following steps to get
it running in Android Studio (and to add the OpenTok Android SDK):

1. In Android Studio, select the File > Import Project command. Navigate to the root directory of
   this project, select the build.gradle file, and then click the OK button. The project opens in a
   new window.

   The Java code for the application is the ChatActivity class in the
   com.tokbox.android.demo.learningopentok package.

2. Download the [OpenTok Android SDK](https://tokbox.com/opentok/libraries/client/android/).

3. Locate the opentok-android-sdk-2.4.0.jar file in the OpenTok/libs directory of the OpenTok
   Android SDK, and drag it into the app/libs directory of the Android Studio project explorer.

4. If the app/src/main/jniLibs directory does not exist in the Android Studio
   project explorer, right-click the app/src/main directory and select the New Resource Directory
   command, enter jniLibs as the directory name, and then click OK.

6. Locate the armeabi and x86 directories in the OpenTok/libs directory of the OpenTok
   Android SDK, and drag them into the app/src/main/jniLibs directory of the Android Studio
   project explorer.

6. Debug the project on a supported device.

   For a list of supported devices, see the "Developer and client requirements"
   on [this page] [1].

## basics.step-2: Creating a session and defining archive REST API calls (server side)

Before you can test the application, you need to set up a web service to handle some
OpenTok-related API calls. The web service securely creates an OpenTok session.

The [Learning OpenTok PHP](https://github.com/opentok/learning-opentok-php) repo includes code
for setting up a web service that handles the following API calls:

* "/service" -- The Android client calls this endpoint to get an OpenTok session ID, token,
  and API key.

* "/start" -- The Android client calls this endpoint to start recording the OpenTok session to
  an archive.

* "/stop" -- The Android client calls this endpoint to stop recording the archive.

* "/view" -- The Android client load this endpoint in a web browser to display the archive
  recording.

Download the repo and run its code on a PHP-enabled web server,

The HTTP POST request to the /service endpoint returns a response that includes the OpenTok
session ID and token.

## basics.step-3: Generating a token (server side)

The web service also creates a token that the client uses to connect to the OpenTok session.
The HTTP GET request to the /service endpoint returns a response that includes the OpenTok
session ID and token.

You will want to authenticate each user (using your own server-side authentication techniques)
before sending an OpenTok token. Otherwise, malicious users could call your web service and
use tokens, causing streaming minutes to be charged to your OpenTok developer account. Also,
it is a best practice to use an HTTPS URL for the web service that returns an OpenTok token,
so that it cannot be intercepted and misused.

## basics.step-4: Connecting to the session

The code for this section is added in the basics.step-4 branch of the repo.

First, set the app to use the web service described in the previous two sections:

1. Open the WebServiceCoordinator.java file. This is in the com.tokbox.android.demo.learningopentok
   package.

2. Edit the `CHAT_SERVER_URL` and `SESSION_INFO_ENDPOINT` values to match the URL and end-point
   of the web service:

        private static final String CHAT_SERVER_URL = "https://example.com";
        private static final String SESSION_INFO_ENDPOINT = CHAT_SERVER_URL + "/session";

You can now test the app in the debugger. On successfully connecting to the session, the
app logs "Session Connected" to the debug console.

The `onCreate()` method of the main ChatActivity object instantiates a WebServiceCoordinator
object and calls its `fetchSessionConnectionData()` method. This method makes an API call to
the /session endpoint of the web service to obtain the OpenTok API key, session ID, and a token
to connect to the session.

Once the session ID is obtained, the WebServiceCoordinator calls the
`onSessionConnectionDataReady()` method of the ChatActivity object, passing in the OpenTok API key,
session ID, and token. This method sets the properties to these values and then calls the
`initializeSession()` method:

    private void initializeSession() {
      mSession = new Session(this, mApiKey, mSessionId);
      mSession.setSessionListener(this);
      mSession.connect(mToken);
    }

The Session class is defined by the OpenTok Android SDK. It represents the OpenTok session
(which connects users).

This app sets `this` as the listener for Session events, defined by the Session.SessionListener 
interface. The ChatActivity class implements this interface, and overrides its methods, such as
the `onConnected(Session session)` method:

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");
    }

The Session.SessionListener interface also defined methods for handling other session-related
events, which we will look at in the following sections.

Finally, the `connect(token)` method of the Session object connects the app to the OpenTok session.
You must connect before sending or receiving audio-video streams in the session (or before
interacting with the session in any way).

## basics.step-5: Publishing an audio video stream to the session

The code for this section is added in the basics.step-5 branch of the repo.

First, let's test the code in this branch:

1. Find the test.html file in the root of the project. You will use the test.html file to
   connect to the OpenTok session and view the audio-video stream published by the Android app:

   * Edit the test.html file and set the `sessionCredentialsUrl` variable to match the
     `ksessionCredentialsUrl` property used in the Android app.

   * Add the test.html file to a web server. (You cannot run WebRTC videos in web pages loaded
     from the desktop.)

   * In a browser, load the test.html file from the web server.

2. Run the Android app. The Android app publishes an audio-video stream to the session and the
   the web client app subscribes to the stream.

Now lets look at the Android code. In addition to initializing and connecting to the session, the
`onSessionConnectionDataReady()` method calls the `initializePublisher()` method:

      private void initializePublisher() {
          mPublisher = new Publisher(this);
          mPublisher.setPublisherListener(this);
          mPublisher.setCameraListener(this);
          mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                  BaseVideoRenderer.STYLE_VIDEO_FILL);
          mPublisherViewContainer.addView(mPublisher.getView());
      }

The Publisher object is defined in the OpenTok Android SDK. A Publisher object acquires
an audio and video stream from the device's microphone and camera. These can then be published
to the OpenTok session as an audio-video stream.

The Publisher class is a subclass of the PublisherKit class, also defined in the OpenTok Android
SDK. The PublisherKit class lets you define custom video drivers (capturers and renderers). The
Publisher class uses the device's camera as as the video source, and it implements a pre-built
video capturer and renderer.

The ChatActivity object sets itself to implement the PublisherKit.PublisherListener interface.
As such it implements method of that interface to handle publisher-related events:

      mPublisher.setPublisherListener(this);

The following code sets the Publisher to scale the video to fill the entire area of the
renderer, with cropping as needed:

      mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
              BaseVideoRenderer.STYLE_VIDEO_FILL);

The 'getView()` method of the Publisher object returns a View object that displays
a view of the camera. This code displays that view in the app:

      mPublisherViewContainer.addView(mPublisher.getView());

The `mPublisherViewContainer` object is a FrameLayout object set to the `publisher_container`
view defined in the main layout XML file.

Upon successfully connecting to the OpenTok session (see the previous section), the
`onConnected(Session session)` method is called. In this branch of the repo, this method
includes a call to the `publish(publisherKit)` method of the Session object:

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");

        if (mPublisher != null) {
            mSession.publish(mPublisher);
        }
    }

The `publish(publisherKit)` method of the Session object publishes an audio-video stream to
the OpenTok session.

Upon successfully publishing the stream, the implementation of the
`onStreamCreated(publisherKit, stream)`  method (defined in the PublisherKit.PublisherListener
interface) is called:

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher Stream Created");
    }

If the publisher stops sending its stream to the session, the implementation of the
`onStreamDestroyed(publisherKit, stream)` method is called:

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher Stream Destroyed");
    }

## basics.step-6: Subscribing to another client's audio-video stream

The code for this section is added in the basics.step-6 branch of the repo.

First, let's test the code in this branch:

1. Find the test.html file in the root of the project. You will use the test.html file to
   connect to the OpenTok session and view the audio-video stream published by the Android app:

   * Edit the test.html file and set the `sessionCredentialsUrl` variable to match the
     `ksessionCredentialsUrl` property used in the Android app.

   * Add the test.html file to a web server. (You cannot run WebRTC videos in web pages loaded
     from the desktop.)

   * In a browser, load the test.html file from the web server.

2. Run the Android app. The Android app subscribes to the audio-video stream published by the
   web page.

The `onStreamReceived(Session session, Stream stream)` method (defined in the
Session.SessionListener interface) is called when a new stream is created in the session.
The app implements this method with the following:

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");

        if (mSubscriber == null) {
            mSubscriber = new Subscriber(this, stream);
            mSubscriber.setSubscriberListener(this);
            mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL);
            mSession.subscribe(mSubscriber);
        }
    }

The method is passed a Session and Stream object, which are both defined by the OpenTok Android
SDK. The Stream object represents the stream that another client is publishing. Although this app
assumes that only one other client is connecting to the session and publishing, the method checks
to see if the app is already subscribing to a stream (if the `mSubscriber` property is null).
If not, the method initializes an Subscriber object (`mSubscriber`), used to subscribe to the
stream, passing in the OTStream object to the constructor function. It also sets the ChatActivity
object as the implementor of the SubscriberKit.SubscriberListener interface. This interface defines
methods that handle events related to the subscriber.

The Subscriber class is also defined in the OpenTok Android SDK. It is a subclass of SubscriberKit,
which lets you define a custom video renderer. The Subscriber object implements a built-in video
renderer.

The following code sets the Subscriber to scale the video to fill the entire area of the
renderer, with cropping as needed:

      mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
              BaseVideoRenderer.STYLE_VIDEO_FILL)

The app then calls the `subscribe(SubscriberKit)` method of the Session object to have the app
subscribe to the stream.

When the app starts receiving the subscribed stream, the implementation of the
`onConnected(subscriberKit)` method (defined by the SubscriberKit.SubscriberListener interface)
is called:

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber Connected");

        mSubscriberViewContainer.addView(mSubscriber.getView());
    }

It adds view of the subscriber stream (returned by the `getView()` method of the Subscriber object)
as a subview of the `mSubscriberViewContainer` View object.

If the subscriber's stream is dropped from the session (perhaps the client chose to stop publishing
or to disconnect from the session), the implementation of the
`Session.SessionListener.onStreamDropped(session, stream)` method is called:

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber != null) {
            mSubscriber = null;
            mSubscriberViewContainer.removeAllViews();
        }
    }

## archiving.step.1

The code for this section is added in the archiving.step-1 branch of the repo. This code builds
upon the code in the basics.step-6 branch of the repo.

The OpenTok archiving API lets you record audio-video streams in a session to MP4 files. You use
server-side code to start and stop archive recordings. In the WebServiceCoordinator file, you set
the following property to the base URL and endpoints of the web service the app calls to start
archive recording, stop recording, and play back the recorded video:

    private static final String CHAT_SERVER_URL = BuildConfig.CHAT_SERVER_URL;
    private static final String SESSION_INFO_ENDPOINT = CHAT_SERVER_URL + "/session";
    private static final String ARCHIVE_START_ENDPOINT = CHAT_SERVER_URL + "/start/:sessionId";
    private static final String ARCHIVE_STOP_ENDPOINT = CHAT_SERVER_URL + "/stop/:archiveId";
    private static final String ARCHIVE_PLAY_ENDPOINT = CHAT_SERVER_URL + "/view/:archiveId"

When the user selects the Start Archive, Stop Archive, and Play Archive menu items from the action 
bar or the options menu, the app calls the `startArchive()` and `stopArchive()`, and `playArchive()`
methods. These call web services that call server-side code start and stop archive recordings.
(See [Creating a session and defining archive REST API calls](#1-creating-a-session-and-defining-archive-REST-API-calls).)

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_start_archive:
                startArchive();
                return true;
            case R.id.action_stop_archive:
                stopArchive();
                return true;
            case R.id.action_play_archive:
                playArchive();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


Note that the ChatActivity class now implements the Session.ArchiveListener interface. This
means that it implements methods for handling archive-related events.

When archive recording starts, the implementation of the
`onArchiveStarted(Session session, String archiveId, String archiveName)` method (defined
by the Session.ArchiveListener interface) is called:

    @Override
    public void onArchiveStarted(Session session, String archiveId, String archiveName) {
        mCurrentArchiveId = archiveId;
        setStopArchiveEnabled(true);
        mArchivingIndicatorView.setVisibility(View.VISIBLE);
    }

The method stores the archive ID (identifying the archive) to an `mCurrentArchiveId` property.
The method also calls the `setStopArchiveEnabled(true)` method, which causes the Stop Recording
menu item to be displayed. And it causes the `mArchivingIndicatorView` to be displayed (which
displays an archiving indicator image).

When the user selects the Stop Archive command, the app passes the archive ID along to the
web service that stops the archive recording.

When archive recording stops, the implementation of the
`onArchiveStopped(Session session, String archiveId)` method (defined
by the Session.ArchiveListener interface) is called:

    @Override
    public void onArchiveStopped(Session session, String archiveId) {
        mPlayableArchiveId = archiveId;
        mCurrentArchiveId = null;
        setPlayArchiveEnabled(true);
        setStartArchiveEnabled(true);
        mArchivingIndicatorView.setVisibility(View.INVISIBLE);
    }

The method stores the archive ID (identifying the archive) to an `mPlayableArchiveId` property
(and sets `mCurrentArchiveId` to `null`). The method also calls the `setPlayArchiveEnabled(false)`
method, which disables the Play Archive menu item, and it calls `setStartArchiveEnabled(true)` to
enable the Start Archive menu item. And it causes the `mArchivingIndicatorView` to be hidden.

When the user clicks the Play Archive button, the `playArchive()` method
opens a web page (in the device's web browser) that displays the archive recording, by calling
the archive playback REST API, defined in [Creating a session and defining archive REST API calls](#1-creating-a-session-and-defining-archive-REST-API-calls).

## signaling.step-1

The code for this section is added in the signaling.step-1 branch of the repo. This code builds
upon the code in the basics.step-6 branch of the repo.

The OpenTok signaling API lets clients send text messages to other clients connected to the
OpenTok session. You can send a signal message to a specific client, or you can send
a message to every client connected to the session.

In this branch, the following code is added to the `initializeSession()` method:

    mSession.setSignalListener(this);

This sets the ChatActivity object as the implementor of the SubscriberKit.SignalListener interface. This interface defines the `onSignalReceived(session, type, data, connection)` methods. This method
is called when the client receives a signal from the session:

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {
        Toast toast = Toast.makeText(this, data, Toast.LENGTH_LONG);
        toast.show();
    }

This app uses an android.widget.Toast object to display received signals.

In the `onConnected(session)` method, the following code sends a signal when the app connects to the
session:

    mSession.sendSignal("", "Hello, Signaling!");

This signal is sent to all clients connected to the session. The method has two parameters:

* `type` (String) -- An optional parameter that can be used as a filter for types of signals.

* `data` (String) -- The data to send with the signal.

## signaling.step-2

The code for this section is added in the signaling.step-2 branch of the repo.

In this branch, the following code is added to the `initializeSession()` method:

    mSendButton = (Button)findViewById(R.id.send_button);
    mMessageEditText = (EditText)findViewById(R.id.message_edit_text);

    // Attach handlers to UI
    mSendButton.setOnClickListener(this);

The main layout XML file adds a Button and and EditText element to the main view. This code adds
properties to reference these objects. It also sets the ChatActivity object as the implementor of
the View.OnClickListener interface. This interface defines the `onClick(View v)` method.

In the `onConnected(Session session)` method (called when the app connects to the OpenTok session)
the following line of code is added in this branch:

    enableMessageViews();

The `enableMessageViews()` method enables the Message text field and the Send button:

    private void enableMessageViews() {
        mMessageEditText.setEnabled(true);
        mSendButton.setEnabled(true);
    }

The `onClick(View v)` method is called when the clicks the Send button:

    @Override
    public void onClick(View v) {
        if (v.equals(mSendButton)) {
            sendMessage();
        }
    }

The `sendMessage()` method sends the text chat message (defined in the Message text field)
to the OpenTok session:

    private void sendMessage() {
        disableMessageViews();
        mSession.sendSignal(SIGNAL_TYPE_MESSAGE, mMessageEditText.getText().toString());
        mMessageEditText.setText("");
        enableMessageViews();
    }

Note that in this branch, the `type` of the signal is set to `SIGNAL_TYPE_MESSAGE`
(a string defined as "message"). The `onSignalReceived()` method checks to see if the
signal received is of this type:

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {
        switch (type) {
            case SIGNAL_TYPE_MESSAGE:
                showMessage(data);
                break;
        }
    }

## signaling.step-3

The code for this section is added in the signaling.step-3 branch of the repo.

First, let's test the code in this branch:

1. Find the test.html file in the root of the project. You will use the test.html file to
   connect to the OpenTok session and view the audio-video stream published by the Android app:

   * Edit the test.html file and set the `sessionCredentialsUrl` variable to match the
     `ksessionCredentialsUrl` property used in the Android app.

   * Add the test.html file to a web server. (You cannot run WebRTC videos in web pages loaded
     from the desktop.)

   * In a browser, load the test.html file from the web server.

2. Run the Android app. Enter some chat message in the Message text field, then click the Send
   button. The web page displays the text message sent by the Android app. You can also send a
   message from the web page to the Android app.

Instead of using a Toast object to display received signals, the code in this branch uses an
android.widget.ListView object. This lets the app display more than one message at a time.
This branch adds the following code to the `onCreate()` method:

    mMessageHistoryListView = (ListView)findViewById(R.id.message_history_list_view);

    // Attach data source to message history
    mMessageHistory = new ChatMessageAdapter(this);
    mMessageHistoryListView.setAdapter(mMessageHistory);

This branch adds code that differentiates between signals (text chat messages) sent from the local
Android client and those sent from other clients connected to the session. The
`onSignalReceived(session, type, data, connection)` method checks the Connection object for
the received signal with the Connection object returned by `mSession.getConnection()`:

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {
    boolean remote = !connection.equals(mSession.getConnection());
        switch (type) {
            case SIGNAL_TYPE_MESSAGE:
                showMessage(data);
                showMessage(data, remote);
                break;
        }
    }

The Connection object of the received signal represents the connection to the session for the client
that sent the signal. This will only match the Connection object returned by
`mSession.getConnection()` if the signal was sent by the local client.

The `showMessage(messageData, remote)` method has a new second parameter: remote. This is set
to `true` if the message was sent another client (and `false` if it was sent by the local
Android client):

    private void showMessage(String messageData, boolean remote) {
        ChatMessage message = ChatMessage.fromData(messageData);
        message.setRemote(remote);
        mMessageHistory.add(message);
     }

The `ChatMessage.fromData()` method converts the message data (the data in the received signal)
into a ChatMessage object. The mMessageHistoryListView uses the mMessageHistory object as
the adaptor for the data in the list view. The mMessageHistory property is an
android.widget.ArrayAdapter object. This tutorial focuses on the OpenTok Android SDK API. For more
information on the Android classes used in this text chat implementation, see the docs for the
following:

* [ArrayAdaptor] [2]
* [ListView] [3]


## audio-driver.step-1

To see the code for this sample, switch to the audio-driver.step-1 branch. This branch shows
you how to implement a custom audio driver and use a simple audio capturer for audio used by
the stream published by the app.

The OpenTok Android SDK lets you set up a custom audio driver for publishers and subscribers. You
can use a custom audio driver to customize the audio sent to a publisher's stream. You can also
customize the playback of subscribed streams' audio.

This sample application uses the custom audio driver to publish white noise (a random audio signal)
to its audio stream. It also uses the custom audio driver to capture the audio from subscribed
streams and save it to a file.

### Setting up the audio device and the audio bus

In using a custom audio driver, you define a custom audio driver and an audio bus to be
used by the app.

The BasicAudioDevice class defines a basic audio device interface to be used by the app.
It extends the BaseAudioDevice class, defined by the OpenTok Android SDK. To use a custom
audio driver, call the `AudioDeviceManager.setAudioDevice(device)` method. This sample sets
the audio device to an instance of the BasicAudioDevice class:

    AudioDeviceManager.setAudioDevice(new BasicAudioDevice(this));

Use the AudioSettings class, defined in the OpenTok Android SDK, to define the audio format used
by the custom audio driver. The `BasicAudioDevice()` constructor instantiates two AudioSettings
instances -- one for the custom audio capturer and one for the custom audio renderer. It sets
the sample rate and number of channels for each:

    public BasicAudioDevice(Context context) {
        mContext = context;

        mCaptureSettings = new AudioSettings(SAMPLING_RATE, NUM_CHANNELS_CAPTURING);
        mRendererSettings = new AudioSettings(SAMPLING_RATE, NUM_CHANNELS_RENDERING);

        mCapturerStarted = false;
        mRendererStarted = false;

        mAudioDriverPaused = false;

        mCapturerHandler = new Handler();
    }

The constructor also sets up some local properties that report whether the device is capturing
or rendering. It also sets a Handler instance to process the `mCapturer` Runnable object.

The `BasicAudioDevice getAudioBus()` method gets the AudioBus instance that this audio device uses,
defined by the BasicAudioDevice.AudioBus class. Use the AudioBus instance to send and receive audio
samples to and from a session. The publisher will access the
AudioBus object to obtain the audio samples. And subscribers will send audio samples (from
subscribed streams) to the AudioBus object.

### Capturing audio to be used by a publisher

The `[OTAudioDevice startCapture:]` method is called when the audio device should start capturing
audio to be published. The BasicAudioDevice implementation of this method starts the `mCapturer`
thread to be run in the queue after 1 second:

    public boolean startCapturer() {
        mCapturerStarted = true;
        mCapturerHandler.postDelayed(mCapturer, mCapturerIntervalMillis);
        return true;
    }

The `mCapturer` thread produces a buffer containing samples of random data (white noise). It then
calls the `writeCaptureData(data, numberOfSamples)` method of the AudioBus object, which sends the
samples to the audio bus. The publisher in the application uses the samples sent to the audio bus to
transmit as audio in the published stream. Then if a capture is still in progress (if
the app is publishing), the `mCapturer` thread is run again after another second:

    private Runnable mCapturer = new Runnable() {
        @Override
        public void run() {
            mCapturerBuffer.rewind();

            Random rand = new Random();
            rand.nextBytes(mCapturerBuffer.array());

            getAudioBus().writeCaptureData(mCapturerBuffer, SAMPLING_RATE);

            if(mCapturerStarted && !mAudioDriverPaused) {
                mCapturerHandler.postDelayed(mCapturer, mCapturerIntervalMillis);
            }
        }
    };

See the next step, audio-driver.step-2, to see a simple implementation of a custom
audio renderer.

### Other notes on the audio driver API

The AudioDevice class includes other methods that are implemented by the BasicAudioDevice class.
However, this sample does not do anything interesting in these methods, so they are not included
in this discussion.


## audio-driver.step-2

To see the code for this sample, switch to the audio-driver.step-2 branch. This branch shows
you how to implement simple audio renderer for subscribed streams' audio.

The `BasicAudioDevice()` constructor method sets up a file to save the incoming audio to a file.
This is done simply to illustrate a use of the custom audio driver's audio renderer.
The app requires the following permissions, defined in the AndroidManifest.xml file:

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

The `BaseAudioDevice initRenderer()` method is called when the app initializes the audio renderer.
The BasicAudioDevice implementation of this method instantiates a new File object, to which
the the app will write audio data:

     @Override
     public boolean initRenderer() {
        mRendererBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2); // Each sample has 2 bytes
        mRendererFile =
          new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                   , "output.raw");
        if (!mRendererFile.exists()) {
            try {
                mRendererFile.getParentFile().mkdirs();
                mRendererFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

The `BaseAudioDevice.startRendering()` method is called when the audio device should start rendering
(playing back) audio from subscribed streams. The BasicAudioDevice implementation of this method
starts the `mCapturer` thread to be run in the queue after 1 second:

     @Override
     public boolean startRenderer() {
         mRendererStarted = true;
         mRendererHandler.postDelayed(mRenderer, mRendererIntervalMillis);
         return true;
     }

The `mRenderer` thread gets 1 second worth of audio from the audio bus by calling the
`readRenderData(buffer, numberOfSamples)` method of the AudioBus object. It then writes the audio
data to the file (for sample purposes). And, if the audio device is still being used to render audio
samples, it sets a timer to run the `mRendererHandler` thread again after 0.1 seconds:

    private Handler mRendererHandler;
    private Runnable mRenderer = new Runnable() {
        @Override
        public void run() {
            mRendererBuffer.clear();
            getAudioBus().readRenderData(mRendererBuffer, SAMPLING_RATE);
            try {
                FileOutputStream stream = new FileOutputStream(mRendererFile);
                stream.write(mRendererBuffer.array());
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (mRendererStarted && !mAudioDriverPaused) {
                mRendererHandler.postDelayed(mRenderer, mRendererIntervalMillis);
            }

        }
    };

This example is intentionally simple for instructional purposes -- it simply writes the audio data
to a file. In a more practical use of a custom audio driver, you could use the custom audio driver
to play back audio to a Bluetooth device or to process audio before playing it back.

# Basic Video Renderer

To see the code for this sample, switch to the basic-renderer branch. This branch shows you
how to make minor modifications to the video renderer used by a Subscriber object. You can also
use the same techniques to modify the video renderer used by a Publisher object (though this
example only illustrates a custom renderer for a subscriber).

In this example, the app uses a custom video renderer to display a black-and-white version of the
Subscriber object's video.

BlackWhiteVideoRender is a custom class that extends the BaseVideoRenderer protocol (defined
in the OpenTok Android SDK). The BaseVideoRenderer class lets you define a custom video renderer
to be used by an OpenTok publisher or subscriber.

    mSubscriberRenderer = new BlackWhiteVideoRender(this);

In the main ChatActivity class, after initializing a Subscriber object, the `setRenderer(renderer)`
method of the Subscriber object is called to set the custom video renderer for the subscriber:

    mSubscriber = new Subscriber(this, stream);
    mSubscriber.setRenderer(mSubscriberRenderer);

The `BlackWhiteVideoRender()` constructor sets a `mRenderView` property to a GLSurfaceView object.
The app uses this object to display the video using OpenGL ES 2.0. The renderer for this
GLSurfaceView object is set to a GLRendererHelper object. GLRendererHelper is a custom class that
extends GLSurfaceView.Renderer, and it is used to render the subscriber video to the GLSurfaceView
object:

    public BlackWhiteVideoRender(Context context) {
        mRenderView = new GLSurfaceView(context);
        mRenderView.setEGLContextClientVersion(2);

        mRenderer = new GLRendererHelper();
        mRenderView.setRenderer(mRenderer);

        mRenderView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

The GLRendererHelper class includes code that converts a video frame to
a black-and-white representation.

The `BaseVideoRenderer.onFrame()` method is called when the subscriber (or subscriber) renders
a video frame to the video renderer. The frame is an BaseVideoRenderer.Frame object (defined by
the OpenTok Android SDK).  In the BlackWhiteVideoRender implementation of this method, it takes
the frame's image buffer, which is a YUV representation of the frame, and transforms it into
black-and-white. It then passes the buffer to the `displayFrame()` method of the GLRendererHelper
object and calls the `requestRender()` method of the GLSurfaceView object:

    @Override
    public void onFrame(Frame frame) {
        ByteBuffer imageBuffer = frame.getBuffer();

        // Image buffer is represented using three planes, Y, U and V.
        // Data is laid out in a linear way in the imageBuffer variable
        // Y plane is first, and its size is the same of the image (width * height)
        // U and V planes are next, in order to produce a B&W image, we set both
        // planes with the same value.

        int startU = frame.getWidth() * frame.getHeight();
        for (int i = startU; i < imageBuffer.capacity(); i++) {
            imageBuffer.put(i, (byte)-127);
        }

        mRenderer.displayFrame(frame);
        mRenderView.requestRender();
    }

The GLRendererHelper class renders the frame contents to an OpenGL surface in Android.


# Basic Video Capturer

To see the code for this sample, switch to the basic-capturer branch. This branch shows you
how to make minor modifications to the video capturer used by the Publisher class.

In this example, the app uses a custom video capturer to publish random pixels (white noise).
This is done simply to illustrate the basic principals of setting up a custom video capturer.
(For a more practical example, see the Camera Video Capturer and Screen Video Capturer examples,
described in the sections that follow.)

In the `initializePublisher()` method ChatActivity class, after creating a Publisher object,
the code calls the `setCapturer(capturer)` method of the Publisher object, passing in a
NoiseVideoCapturer object:

    mPublisher.setCapturer(new NoiseVideoCapturer(320, 240));

NoiseVideoCapturer is a custom class that extends the BaseVideoCapturer class (defined
in the OpenTok iOS SDK). This class lets you define a custom video capturer to be used
by an OpenTok publisher.

The `BaseVideoCapturer.init()` method initializes capture settings to be used by the custom
video capturer. In this sample's custom implementation of BaseVideoCapturer (NoiseVideoCapturer)
the `initCapture()` method sets properties of a `mCapturerSettings` property:

    @Override
    public void init() {
        mCapturerHasStarted = false;
        mCapturerIsPaused = false;

        mCapturerSettings = new CaptureSettings();
        mCapturerSettings.height = mHeight;
        mCapturerSettings.width = mWidth;
        mCapturerSettings.format = BaseVideoCapturer.ARGB;
        mCapturerSettings.fps = FPS;
        mCapturerSettings.expectedDelay = 0;
    }

The BaseVideoCapturer.CaptureSettings class (which defines this `mCapturerSettings` property)
is defined by the OpenTok Android SDK. In this sample code, the format of the video capturer is
set to use ARGB as the pixel format, with a specific number of frames per second, a specific height,
and a specific width.

The `[OTVideoCapture setVideoCaptureConsumer]` sets an OTVideoCaptureConsumer object (defined
by the OpenTok iOS SDK) the the video consumer uses to transmit video frames to the publisher's
stream. In the OTKBasicVideoCapturer, this method sets a local OTVideoCaptureConsumer instance
as the consumer:

    - (void)setVideoCaptureConsumer:(id<OTVideoCaptureConsumer>)videoCaptureConsumer
    {
        // Save consumer instance in order to use it to send frames to the session
        self.consumer = videoCaptureConsumer;
    }

The `BaseVideoCapturer startCapture()` method is called when a publisher starts capturing video
to send as a stream to the OpenTok session. This will occur after the `Session.publish(publisher)`
method is called. In the NoiseVideoCapturer implementation of this method, the `run()`
`mFrameProducer` thread is started:

    @Override
    public int startCapture() {
        mCapturerHasStarted = true;
        mFrameProducer.run();
        return 0;
    }

The `[self produceFrame]` method creates a buffer of bytes and fills it with random noise.
(Note that each frame is four bytes in the buffer, defining the constituent ARGB values for
the frame.) It then passes the buffer into the
`provideByteArrayFrame(data, format, width, height, rotation, mirrorX)` method, defined by the
BaseVideoCapturer class (in the OpenTok Android SDK):

    Runnable mFrameProducer = new Runnable() {
        @Override
        public void run() {
            Random random = new Random();
            byte[] buffer = new byte[mWidth * mHeight * 4];
            byte[] randoms = new byte[4];
            for (int i = 0; i < mWidth * mHeight * 4; i += 4) {
                random.nextBytes(randoms);
                buffer[i] = randoms[0];
                buffer[i + 1] = randoms[1];
                buffer[i + 2] = randoms[2];
                buffer[i + 3] = randoms[3];
            }

            provideByteArrayFrame(buffer, BaseVideoCapturer.ARGB,
                    mWidth, mHeight, Surface.ROTATION_0, false);

            if (mCapturerHasStarted && !mCapturerIsPaused) {
                mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
            }
        }
    };

This causes the publisher to send the frame of data to the video stream in the session.
If the session is still publishing data, the `mFrameProducer` thread is run again
after a specified delay (`mFrameProducerIntervalMillis`), causing another frame of video to be
captured and published.


# Camera video capturer

To see the code for this sample, switch to the camera-capturer branch. This branch shows you
how to use a custom video capturer using the device camera as the video source.

Before studying this sample, see the basic-capturer sample.

In the `initializePublisher()` method of the ChatActivity class, after creating a Publisher object,
the code calls the `setCapturer(capturer)` method of the Publisher object, passing in a
CameraVideoCapturer object:

    mPublisher.setCapturer(new CameraVideoCapturer(this, 640, 480, 30));

CameraVideoCapturer is a custom class that extends the BaseVideoCapturer class (defined
in the OpenTok iOS SDK). This class lets you define a custom video capturer to be used
by an OpenTok publisher. The CameraVideoCapturer class uses a FrontCameraFrameProvider
object to capture frames from the Android camera:

    public CameraVideoCapturer(Context context, int width, int height, int fps) {
        mWidth = width;
        mHeight = height;
        mDesiredFps = fps;

        mHelper = new FrontCameraFrameProvider(context, mWidth, mHeight, mDesiredFps);
        mHelper.setHelperListener(this);
    }

The FrontCameraFrameProvider class uses a FrontCameraFrameProvider.ProviderListener interface
to send events when frames are available for capture. The CameraVideoCapturer class
implements this interface and its `cameraFrameReady()` method.

The `BaseVideoCapturer.init()` method initializes capture settings to be used by the custom
video capturer. In this sample's custom implementation of BaseVideoCapturer (CameraVideoCapturer)
the `initCapture()` it also calls the `init()` method of the FrontCameraFrameProvider class:

    @Override
    public void init() {
        mCapturerHasStarted = false;
        mCapturerIsPaused = false;

        mCapturerSettings = new CaptureSettings();
        mCapturerSettings.height = mHeight;
        mCapturerSettings.width = mWidth;
        mCapturerSettings.expectedDelay = 0;
        mCapturerSettings.format = BaseVideoCapturer.NV21;

        mHelper.init();
    }

The `FrontCameraFrameProvider.init()` method creates a Camera object.

The `BaseVideoCapturer.startCapture()` method of is called when a Publisher using the video capturer
starts capturing video to send as a stream to the OpenTok session. This will occur after the
`Session.publish(publisher)` method is called. The CameraVideoCapturer of this method calls the
`startCapture()` method of the FrontCameraFrameProvider object:

    @Override
    public int startCapture() {
        mCapturerHasStarted = true;
        mHelper.startCapture();
        return 0;
    }

The `FrontCameraFrameProvider.startCapture()` method sets some parameters of the Android Camera,
based on parameters set at initialization. It creates a callback buffer for the camera (calling
the `Camera.addCallbackBuffer(buffer)` method). It also sets itself as the implementer of the
Camera.PreviewCallback interface, and it calls the `startPreview()` method of the Camera object:

    mCamera.setPreviewCallbackWithBuffer(this);
    mCamera.startPreview();

The FrontCameraFrameProvider implementation of the
`FrontCameraFrameProvider.onPreviewFrame(data, camera)` method. This method is called when
preview frames are displayed by the camera. The FrontCameraFrameProvider calls the
`cameraFrameReady()` method of the ProviderListener object, which is the CameraVideoCapturer
object in this app:

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mPreviewBufferLock.lock();
        if (data.length == mExpectedFrameSize) {
            // Get the rotation of the camera
            int currentRotation = compensateCameraRotation(mCurrentDisplay
                    .getRotation());

            if (mListener != null) {
                mListener.cameraFrameReady(data, mCaptureActualWidth, mCaptureActualHeight,
                        currentRotation, isFrontCamera());
            }

            // Reuse the video buffer
            camera.addCallbackBuffer(data);
        }
        mPreviewBufferLock.unlock();
    }


The CameraVideoCapturer implementation of the `cameraFrameReady()` method (defined by the
Camera.PreviewCallback interface) calls the `provideByteArrayFrame()` method, defined by the
BaseVideoCapturer class (which the CameraVideoCapturer class extends):

    @Override
    public void cameraFrameReady(byte[] data, int width, int height, int rotation, boolean mirror) {
        provideByteArrayFrame(data, NV21, width,
                height, rotation, mirror);
    }

This sends a byte array of data to the publisher, to be used for the next video frame published.

Note that the CameraVideoCapturer also includes code for determining which camera is being used,
working with the camera orientation and rotation, and determining the image size.


# Screen sharing (screensharing)

To see the code for this sample, switch to the screensharing branch. This branch shows you
how to capture the screen (an Android View) using a custom video capturer.

Before studying this sample, see the basic-capturer-step.1 sample.

This sample code demonstrates how to use the OpenTok Android SDK to publish a screen-sharing video,
using the device screen as the source for the stream's video.

The ChatActivity class uses WebView object as the source for the screen-sharing video in the
published stream.

In the `initializePublisher()` method of the ChatActivity class, after creating a Publisher object
the code calls the `setCapturer(capturer)` method of the Publisher object, passing in a
ScreensharingCapturer object:

    mPublisher.setCapturer(new ScreensharingCapturer(mScreensharedView));

ScreensharingCapturer is a custom class that extends the BaseVideoCapturer class (defined
in the OpenTok iOS SDK). This class lets you define a custom video capturer to be used
by an OpenTok publisher. The constructor of the ScreensharingCapturer class is passed an Android
View object, which it will use as the source for the video:

    public ScreensharingCapturer(View view) {
        mContentView = view;
        mFrameProducerHandler = new Handler();
    }

The constructor also creates a new Handler object to process the `mFrameProducer` Runnable object.

The `initCapture` method is used to initialize the capture and sets value for the pixel format of
an OTVideoFrame object. In this  example, it is set to RGB.

The `BaseVideoCapturer.init()` method initializes capture settings to be used by the custom
video capturer. In this sample's custom implementation of BaseVideoCapturer (ScreensharingCapturer)
the `initCapture()` it also sets some settings for the video capturer:

    @Override
    public void init() {
        mCapturerHasStarted = false;
        mCapturerIsPaused = false;

        mCapturerSettings = new CaptureSettings();
        mCapturerSettings.fps = FPS;
        mCapturerSettings.width = mWidth;
        mCapturerSettings.height = mHeight;
        mCapturerSettings.format = BaseVideoCapturer.ARGB;
    }

The `startCapture()` method starts the `mFrameProducer` thread after 1/15 second:

    @Override
    public int startCapture() {
        mCapturerHasStarted = true;
        mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
        return 0;
    }

The mFrameProducer` thread gets a Bitmap representation of the `mContentView` object
(the WebView), writes its pixels to a buffer, and then calls the `provideIntArrayFrame()`
method, passing in that buffer:

    private Runnable mFrameProducer = new Runnable() {
        @Override
        public void run() {
            int width = mContentView.getWidth();
            int height = mContentView.getHeight();

            if (frameBuffer == null || mWidth != width || mHeight != height) {
                mWidth = width;
                mHeight = height;
                frameBuffer = new int[mWidth * mHeight];
            }

            mContentView.setDrawingCacheEnabled(true);
            mContentView.buildDrawingCache();
            Bitmap bmp = mContentView.getDrawingCache();
            if (bmp != null) {
                bmp.getPixels(frameBuffer, 0, width, 0, 0, width, height);
                mContentView.setDrawingCacheEnabled(false);
                provideIntArrayFrame(frameBuffer, ARGB, width, height, 0, false);
            }

            if (mCapturerHasStarted && !mCapturerIsPaused) {
                mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis);
            }
        }
    };

The `provideIntArrayFrame()` method, defined by the BaseVideoCapturer class (which the
CameraVideoCapturer class extends) sends an integer array of data to the publisher, to be used
for the next video frame published.

If the publisher is still capturing video, the thread then starts again after another 1/15 of a
second, so that the capturer continues to supply the publisher with new video frames to publish.


Other resources
---------------

See the following:

* [API reference] [4] -- Provides details on the OpenTok Android API
* [Tutorials] [5] -- Includes conceptual information and code samples for all OpenTok features

[1]: https://tokbox.com/opentok/libraries/client/android/
[2]: http://developer.android.com/reference/android/widget/ArrayAdapter.html
[3]: http://developer.android.com/reference/android/widget/ListView.html
[4]: https://tokbox.com/opentok/libraries/client/android/reference/
[5]: https://tokbox.com/opentok/tutorials/
