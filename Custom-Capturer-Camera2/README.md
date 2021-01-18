# Custom Video Capturer (camera2 interface)

Sample app shows how to use the custom capturer using [camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary) package. 

> Note: You need to have a minimum API level 21 to run this sample app.

> Note: If you aren't familiar with setting up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/).
## Using a custom video capturer

The `MainActivity` class shows how you can use a custom video capturer for a publisher. After
instantiating a Publisher object, the code sets a custom video capturer as shown below.

```java
CustomVideoCapturerCamera2 baseVideoCapturer 
                    = new CustomVideoCapturerCamera2(MainActivity.this, 
                    Publisher.CameraCaptureResolution.MEDIUM, 
                    Publisher.CameraCaptureFrameRate.FPS_30);
            
mPublisher = new Publisher.Builder(MainActivity.this)
        .name("publisher")
        .capturer(baseVideoCapturer)
        .build();
mPublisher.setPublisherListener(this);
```

The `CustomVideoCapturerCamera2` class is defined in the `com.example.tokbox.CustomVideoDriverLib` package.
This class extends the `BaseVideoCapturer` class, defined in the OpenTok Android SDK.
The `getCaptureSettings()` method returns the settings of the video capturer, including the frame
rate, width, height, video delay, and video format for the capturer:

```java
@Override
public CaptureSettings getCaptureSettings() {

    // Set the preferred capturing size
    configureCaptureSize(PREFERRED_CAPTURE_WIDTH, PREFERRED_CAPTURE_HEIGHT);

    CaptureSettings settings = new CaptureSettings();
    settings.fps = mCaptureFPS;
    settings.width = mCaptureWidth;
    settings.height = mCaptureHeight;
    settings.format = NV21;
    settings.expectedDelay = 0;
    
    return settings;
}
```
## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
