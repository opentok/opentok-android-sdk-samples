# Custom Video Capturer (camera2 interface)
Sample app shows how to use the custom capturer using camera2 package. 
Note: You need to have a minimum api level 21 to run this sample app

## Using a custom video capturer

The MainActivity class shows how you can use a custom video capturer for a publisher. After
instantiating a Publisher object, the code sets a custom video capturer as shown below.

```java
     mPublisher = new Publisher.Builder(MainActivity.this)
                    .name("publisher")
                    .capturer(new CustomVideoCapturerCamera2(MainActivity.this, Publisher.CameraCaptureResolution.MEDIUM,                           Publisher.CameraCaptureFrameRate.FPS_30))
                    .build();
            mPublisher.setPublisherListener(this);
```

The CustomVideoCapturerCamera2 class is defined in the `com.example.tokbox.CustomVideoDriverLib` package.
This class extends the BaseVideoCapturer class, defined in the OpenTok Android SDK.
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
## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
