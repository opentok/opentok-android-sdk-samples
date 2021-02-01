# Custom Audio Driver

This app shows how to use the audio driver API to implement a custom audio capturer and player. Just like the custom video driver, most applications will be fine using the default. If you want to add custom audio manipulation, look here.

> Note: If you aren't familiar with setting up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/).


## Using a custom audio driver

The `MainActivity` class shows how you can use a custom audio driver for publisher and
subscriber audio.

The `MainActivity` class instantiates a `CustomAudioDevice` instance and passes it into the
`AudioDeviceManager.setAudioDevice()` method:

```java
CustomAudioDevice customAudioDevice = new CustomAudioDevice(this);
AudioDeviceManager.setAudioDevice(customAudioDevice);

session = new Session(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
```

The `CustomAudioDevice` class extends the `BaseAudioDevice` class, defined in the 
[OpenTok Android SDK](https://tokbox.com/developer/sdks/android/). This class includes methods for 
setting up and using a custom audio driver. The audio driver contains an audio capturer -- used to get 
audio samples from the audio source -- and an audio renderer -- used to play back audio samples from 
the OpenTok streams the client has subscribed to.

> Note: that you must call the method `AudioDeviceManager.setAudioDevice()` before you instantiate
a Session object (and connect to the session).

The constructor for the `CustomAudioDevice` class instantiates two instances of the
`BaseAudioDevice.AudioSettings` class, defined in the OpenTok Android SDK. These are settings for
audio capturing and audio rendering:

```java
m_captureSettings = new AudioSettings(SAMPLING_RATE,NUM_CHANNELS_CAPTURING);
m_rendererSettings = new AudioSettings(SAMPLING_RATE,NUM_CHANNELS_RENDERING);
```

The CustomAudioDevice class overrides the `initCapturer()` method, defined in the `BaseAudioDevice`
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

The `initCapturer()` method also sets up a thread to capture audio from the device:

```java
new Thread(m_captureThread).start();
```

The CustomAudioDevice overrides the `startCapturer()` method, which is called when the app starts
sampling audio to be sent to the publisher's stream. The audio capture thread reads audio samples
from the AudioRecord object into a buffer, `m_recbuffer`:

```java
int lengthInBytes = (samplesToRec << 1) * NUM_CHANNELS_CAPTURING;
int readBytes = m_audioRecord.read(m_tempBufRec, 0, lengthInBytes);

m_recBuffer.rewind();
m_recBuffer.put(m_tempBufRec);

samplesRead = (readBytes >> 1) / NUM_CHANNELS_CAPTURING;
```

The `getAudioBus()` method, defined in the `BaseAudioDevice` class, returns a `BaseAudioDevice.AudioBus`
object, also defined in the OpenTok Android SDK. This audio bus object includes a
`writeCaptureData()` method, which you call to send audio samples to be used as audio data for the
publisher's stream:

```java
getAudioBus().writeCaptureData(m_recBuffer, samplesRead);
```

The `CustomAudioDevice` class overrides the `initRenderer()` method, defined in the `BaseAudioDevice`
class. This method initializes the app's audio renderer, instantiating an `andriod.media.AudioTrack`
instance. This object will be used to play back audio to the device's audio output hardware:

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

The `initRenderer()` method also sets up a thread to play back audio to the device's audio output
hardware:

```java
new Thread(m_renderThread).start();
```

The C`ustomAudioDevice` overrides the `startRenderer()` method, which is called when the app starts
receiving audio from subscribed streams.

The `AudioBus` object includes a `readRenderData()` method, which the audio render thread calls
to read audio samples from the subscribed streams into a playback buffer:

```java
int samplesRead = getAudioBus().readRenderData(m_playBuffer, samplesToPlay);
```

Sample data is written from the playback buffer to the audio track:

```java
int bytesRead = (samplesRead << 1) * NUM_CHANNELS_RENDERING;
m_playBuffer.get(m_tempBufPlay, 0, bytesRead);

int bytesWritten = m_audioTrack.write(m_tempBufPlay, 0, bytesRead);

// increase by number of written samples
m_bufferedPlaySamples += (bytesWritten >> 1) / NUM_CHANNELS_RENDERING;

// decrease by number of played samples
int pos = m_audioTrack.getPlaybackHeadPosition();
if (pos < m_playPosition) {
        // wrap or reset by driver
        m_playPosition = 0;
}

m_bufferedPlaySamples -= (pos - m_playPosition);
m_playPosition = pos;
```