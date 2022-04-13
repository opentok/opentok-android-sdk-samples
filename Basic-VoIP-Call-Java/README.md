# Basic VoIP Call

This app shows how to use the Android Connection Service for publisher and subscriber audio. 
The sample application uses the custom audio driver to publish white noise (a random audio signal)
to its audio stream.

`NoiseAudioDevice` instance is passed into the `AudioDeviceManager.setAudioDevice` method:

```java
AudioDeviceManager.setAudioDevice(noiseAudioDevice);
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)