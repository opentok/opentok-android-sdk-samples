# Basic Video Capturer Camera 2

Sample app shows how to use the custom capturer using the [Camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary) package. This class was introduced in API level 21 providing alternative for deprecated [Camera](https://developer.android.com/reference/android/hardware/Camera) class.

> Note: If you aren't familiar with setting up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/).

> Note: If you want to support API level below 21 [Basic Video Capturer](../Basic-Video-Capturer) project contains capturer for deprecated [Camera](https://developer.android.com/reference/android/hardware/Camera) class.

> Note: Check [Custom Video Driver](../Custom-Video-Driver) project to see how custom video capturer and custom video renderer works together.

A custom video capturer will be helpful when:
- streaming any content other than the one coming from the device camera, e.g. streaming content of a particular view's game or content.
- modyfying the camera stream content - image composition (adding watermark logo) or image procssing (removing the background or putting a virtual hat on the user).

## Using a custom video capturer

The `MainActivity` class shows how you can use a custom video capturer for a publisher. After
instantiating a Publisher object, the code sets a custom video capturer by calling the
`capturer(BaseVideoCapturer capturer)` method of the Publisher:

```java
mPublisher = new Publisher(this, "publisher")
    .capturer(new MirrorVideoCapturer(this))
    .build();

mPublisher.setPublisherListener(this);

```

The `MirrorVideoCapturer` class extends the `BaseVideoCapturer` class, defined in the OpenTok Android SDK. The `getCaptureSettings()` method returns the settings of the video capturer, including the frame rate, width, height, video delay, and video format for the capturer:

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

The app calls `startCameraCapture()` to start capturing video from the custom video capturer.

The publisher adds this video frames to the published stream.

## Further Reading

* Review [Custom Video Driver](../Custom-Video-Driver) project
* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)


