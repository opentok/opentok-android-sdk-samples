# Advanced Audio Driver

This app shows how to use the audio driver for publisher and subscriber audio. Just like the custom video driver, most applications will be fine using the default. If you want to add custom audio manipulation, look here.

`AdvancedAudioDevice` instance is passed into the `AudioDeviceManager.setAudioDevice` method:

```java
AdvancedAudioDevice AdvancedAudioDevice = new AdvancedAudioDevice(this);
AudioDeviceManager.setAudioDevice(AdvancedAudioDevice);
```

The `AdvancedAudioDevice` class extends the `BaseAudioDevice` class, defined in the 
OpenTok Android SDK. This class includes methods for 
setting up and using a custom audio driver. The audio driver contains an audio capturer -- used to get 
audio samples from the audio source -- and an audio renderer -- used to playback audio samples from 
the OpenTok streams the client has subscribed to.

> Note: that you must call the method `AudioDeviceManager.setAudioDevice()` before you instantiate
a Session object (and connect to the session).

The constructor for the `AdvancedAudioDevice` class instantiates two instances of the
`BaseAudioDevice.AudioSettings` class, defined in the OpenTok Android SDK. These are settings for
audio capturing and audio rendering:

```java
captureSettings = new AudioSettings(captureSamplingRate, NUM_CHANNELS_CAPTURING);
rendererSettings = new AudioSettings(outputSamplingRate, NUM_CHANNELS_RENDERING);
```

The AdvancedAudioDevice class overrides the `initCapturer` method, defined in the `BaseAudioDevice`
class. This method initializes the app's audio capturer, instantiating a an
`andriod.media.AudioRecord` instance to be used to capture audio from the device's audio input
hardware:

```java
int channelConfig = NUM_CHANNELS_CAPTURING == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;

audioRecord = new AudioRecord(
        AudioSource.VOICE_COMMUNICATION,
        captureSettings.getSampleRate(),
        channelConfig,
        AudioFormat.ENCODING_PCM_16BIT, recBufSize);
```

The `initCapturer` method also sets up a thread to capture audio from the device:

```java
new Thread(captureThread).start();
```

The AdvancedAudioDevice overrides the `startCapturer` method, which is called when the app starts
sampling audio to be sent to the publisher's stream. The audio capture thread reads audio samples
from the AudioRecord object into a buffer, `m_recbuffer`:

```java
int lengthInBytes = (samplesToRec << 1) * NUM_CHANNELS_CAPTURING;
int readBytes = audioRecord.read(tempBufRec, 0, lengthInBytes);

recBuffer.rewind();
recBuffer.put(tempBufRec);

samplesRead = (readBytes >> 1) / NUM_CHANNELS_CAPTURING;
```

The `getAudioBus` method, defined in the `BaseAudioDevice` class, returns a `BaseAudioDevice.AudioBus`
object, also defined in the OpenTok Android SDK. This audio bus object includes a
`writeCaptureData` method, which you call to send audio samples to be used as audio data for the
publisher's stream:

```java
getAudioBus().writeCaptureData(recBuffer, samplesRead);
```

The `AdvancedAudioDevice` class overrides the `initRenderer` method, defined in the `BaseAudioDevice`
class. This method initializes the app's audio renderer, instantiating an `andriod.media.AudioTrack`
instance. This object will be used to playback audio to the device's audio output hardware:

```java
int channelConfig = (NUM_CHANNELS_RENDERING == 1) ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

audioTrack = new AudioTrack(
        AudioManager.STREAM_VOICE_CALL,
        rendererSettings.getSampleRate(),
        channelConfig,
        AudioFormat.ENCODING_PCM_16BIT,
        minPlayBufSize >= 6000 ? minPlayBufSize : minPlayBufSize * 2,
        AudioTrack.MODE_STREAM
);
```

The `initRenderer` method also sets up a thread to playback audio to the device's audio output
hardware:

```java
new Thread(m_renderThread).start();
```

The `AdvancedAudioDevice` overrides the `startRenderer` method, which is called when the app starts
receiving audio from subscribed streams.

The `AudioBus` object includes a `readRenderData` method, which the audio render thread calls
to read audio samples from the subscribed streams into a playback buffer:

```java
int samplesRead = getAudioBus().readRenderData(m_playBuffer, samplesToPlay);
```

Sample data is written from the playback buffer to the audio track:

```java
int bytesRead = (samplesRead << 1) * NUM_CHANNELS_RENDERING;
playBuffer.get(tempBufPlay, 0, bytesRead);

int bytesWritten = audioTrack.write(tempBufPlay, 0, bytesRead);

// increase by number of written samples
bufferedPlaySamples += (bytesWritten >> 1) / NUM_CHANNELS_RENDERING;

// decrease by number of played samples
int pos = audioTrack.getPlaybackHeadPosition();
if (pos < playPosition) {
        // wrap or reset by driver
        playPosition = 0;
}

bufferedPlaySamples -= (pos - playPosition);
playPosition = pos;
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)