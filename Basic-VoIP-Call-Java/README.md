# Basic VoIP Call

This app shows how to use the Android Connection Service for publisher and subscriber audio. 
The sample application uses the custom audio driver to publish white noise (a random audio signal)
to its audio stream.

`NoiseAudioDevice` instance is passed into the `AudioDeviceManager.setAudioDevice` method:

```java
AudioDeviceManager.setAudioDevice(noiseAudioDevice);
```

# Configure the app
Open the `OpenTokConfig` file and configure the `API_KEY`, `SESSION_ID`, and `TOKEN` variables. 
You can obtain these values from your [TokBox account](https://tokbox.com/account/#/).

# Android Connection Service
OTConnectionService.java shows how to use the Android ConnectionService class for creating an app
with calling features. 

# Register for Notification services
OTFireBaseMessagingService.java can be used as a reference to create notification services while 
the app is in background mode. 

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
