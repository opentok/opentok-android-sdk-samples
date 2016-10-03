# Project 3: Custom Audio Driver

Note: Read the README.md file in the Project 1 folder before starting here.

## Using a custom audio driver

The MainActivity sample shows how you can use a custom audio driver for publisher and
subscriber audio.

The MainActivity class instantiates a CustomAudioDevice instance and passes it into the
`AudioDeviceManager.setAudioDevice()` method:

```java
            CustomAudioDevice customAudioDevice = new CustomAudioDevice(MainActivity.this);
            AudioDeviceManager.setAudioDevice(customAudioDevice);

            mSession = new Session(MainActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
```

The CustomAudioDevice class extends the BaseAudioDevice class, defined in the OpenTok Android SDK. This class includes methods for setting up and using a custom audio driver. The audio driver
includes an audio capturer -- used to get audio samples from a audio source -- and an audio
renderer -- used to play back audio samples from the OpenTok streams the client has subscribed to.

Note that you must call the method `AudioDeviceManager.setAudioDevice()` before you instantiate
a Session object (and connect to the session).

The constructor for the CustomAudioDevice class instantiates two instances of the
BaseAudioDevice.AudioSettings class, defined in the OpenTok Android SDK. These are settings for
audio capturing and audio rendering:

```java
    m_captureSettings = new AudioSettings(SAMPLING_RATE,
            NUM_CHANNELS_CAPTURING);
    m_rendererSettings = new AudioSettings(SAMPLING_RATE,
            NUM_CHANNELS_RENDERING);
```

The CustomAudioDevice class overrides the `initCapturer()` method, defined in the BaseAudioDevice
class. This method initializes the app's audio capturer, instantiating a an
andriod.media.AudioRecord instance to be used to capture audio from the device's audio input
hardware:

```java
    m_audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
        m_captureSettings.getSampleRate(),
        NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO
                : AudioFormat.CHANNEL_IN_STEREO,
        AudioFormat.ENCODING_PCM_16BIT, recBufSize);
```

The `initCapturer()` method also sets up a thread to capture audio from the device:

```java
    new Thread(m_captureThread).start();
```

The CustomAudioDevice overrides the `startCapturer()` method, which is called when the app starts
sampling audio to be sent to the publisher's stream. The audio capture thread reads audio samples
from the AudioRecord object into a buffer, `m_recbuffer`:

```java
    int lengthInBytes = (samplesToRec << 1)
            * NUM_CHANNELS_CAPTURING;
    int readBytes = m_audioRecord.read(m_tempBufRec, 0,
            lengthInBytes);

    m_recBuffer.rewind();
    m_recBuffer.put(m_tempBufRec);

    samplesRead = (readBytes >> 1) / NUM_CHANNELS_CAPTURING;
```

The `getAudioBus()` method, defined in the BaseAudioDevice class, returns a BaseAudioDevice.AudioBus
object, also defined in the OpenTok Android SDK. This audio bus object includes a
`writeCaptureData()` method, which you call to send audio samples to be used as audio data for the
publisher's stream:

```java
    getAudioBus().writeCaptureData(m_recBuffer, samplesRead);
```

The CustomAudioDevice class overrides the `initRenderer()` method, defined in the BaseAudioDevice
class. This method initializes the app's audio renderer, instantiating an andriod.media.AudioTrack
instance. This object will be used to play back audio to the device's audio output hardware:

```java
    m_audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
            m_rendererSettings.getSampleRate(),
            NUM_CHANNELS_RENDERING == 1 ? AudioFormat.CHANNEL_OUT_MONO
                    : AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT, playBufSize,
            AudioTrack.MODE_STREAM);
```

The `initRenderer()` method also sets up a thread to play back audio to the device's audio output
hardware:

```java
    new Thread(m_renderThread).start();
```

The CustomAudioDevice overrides the `startRenderer()` method, which is called when the app starts
receiving audio from subscribed streams.

The AudioBus object includes a `readRenderData()` method, which the audio render thread calls
to read audio samples from the subscribed streams into a playback buffer:

```java
    int samplesRead = getAudioBus().readRenderData(
            m_playBuffer, samplesToPlay);
```

Sample data is written from the playback buffer to the audio track:

```java
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
```

## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
