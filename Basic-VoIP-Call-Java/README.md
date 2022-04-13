# Basic VoIP Call

This app shows how to use the Android Connection Service for publisher and subscriber audio. This sample application uses the custom audio driver to publish white noise (a random audio signal)
to its audio stream. It also uses the custom audio driver to capture the audio from subscribed streams and save it to a file. Just like the custom video driver, most applications will be fine using the default. If you want to add custom audio manipulation, look here.

`NoiseAudioDevice` instance is passed into the `AudioDeviceManager.setAudioDevice` method:

```java
AudioDeviceManager.setAudioDevice(noiseAudioDevice);
```

```java
public NoiseAudioDevice(Context context) {
    this.context = context;

    captureSettings = new AudioSettings(SAMPLING_RATE, NUM_CHANNELS_CAPTURING);
    rendererSettings = new AudioSettings(SAMPLING_RATE, NUM_CHANNELS_RENDERING);

    capturerStarted = false;
    rendererStarted = false;

    audioDriverPaused = false;

    capturerHandler = new Handler();
    rendererHandler = new Handler();
}
```

The constructor also sets up some local properties that report whether the device is capturing
or rendering. It also sets a Handler instance to process the `capturer` Runnable object.

The `NoiseAudioDevice.getAudioBus` method gets the `AudioBus` instance that this audio device uses,
defined by the `NoiseAudioDevice.AudioBus` class. Use the `AudioBus` instance to send and receive audio
samples to and from a session. The publisher will access the
`AudioBus` object to obtain the audio samples. Subscribers will send audio samples (from
subscribed streams) to the AudioBus object.

### Capturing audio to be used by a publisher

The `BaseAudioDevice.startCapturer` method is called when the audio device should start capturing
audio to be published. The `NoiseAudioDevice` implementation of this method starts the `capturer`
thread to be run in the queue after 1 second:

```java
public boolean startCapturer() {
    capturerStarted = true;
    capturerHandler.postDelayed(capturer, capturerIntervalMillis);
    return true;
}
```

The `capturer` thread produces a buffer containing samples of random data (white noise). It then
calls the `writeCaptureData` method of the `AudioBus` object, which sends the
samples to the audio bus. The publisher in the application uses the samples sent to the audio bus to
transmit as audio in the published stream. Then if a capture is still in progress (if
the app is publishing), the `capturer` thread is run again after another second:

```java
private Runnable capturer = new Runnable() {
    @Override
    public void run() {
        capturerBuffer.rewind();

        Random rand = new Random();
        rand.nextBytes(capturerBuffer.array());

        getAudioBus().writeCaptureData(capturerBuffer, SAMPLING_RATE);

        if(capturerStarted && !audioDriverPaused) {
            capturerHandler.postDelayed(capturer, capturerIntervalMillis);
        }
    }
};
```

The AudioDevice class includes other methods that are implemented by the NoiseAudioDevice class.
However, this sample does not do anything interesting in these methods, so they are not included
in this discussion.



The `BaseAudioDevice.startRendering` method is called when the audio device should start rendering
(playing back) audio from subscribed streams. The `NoiseAudioDevice` implementation of this method
starts the `capturer` thread to be run in the queue after 1 second:

```java 
@Override
 public boolean startRenderer() {
     rendererStarted = true;
     rendererHandler.postDelayed(renderer, rendererIntervalMillis);
     return true;
 }
```

The `renderer` thread gets 1 second worth of audio from the audio bus by calling the
`readRenderData` method of the `AudioBus` object. It then writes the audio
data to the file (for sample purposes). And, if the audio device is still being used to render audio
samples, it sets a timer to run the `rendererHandler` thread again after 0.1 seconds:

```java
private Handler rendererHandler;

private Runnable renderer = new Runnable() {
    @Override
    public void run() {
        rendererBuffer.clear();
        getAudioBus().readRenderData(rendererBuffer, SAMPLING_RATE);

        try {
            FileOutputStream stream = new FileOutputStream(rendererFile);
            stream.write(rendererBuffer.array());
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (rendererStarted && !audioDriverPaused) {
            rendererHandler.postDelayed(renderer, rendererIntervalMillis);
        }
    }
};
```

This example is intentionally simple for instructional purposes -- it simply writes the audio data
to a file. In a more practical use of a custom audio driver, you could use the custom audio driver
to play back audio to a Bluetooth device or to process audio before playing it back.

## Further Reading

* Review [other sample projects](../)
* Review [Advanced-Audio-Driver](../Advanced-Audio-Driver)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)