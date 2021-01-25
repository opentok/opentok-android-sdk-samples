# Basic Video Capturer Camera

Sample app shows how to use the custom capturer using the [Camera](https://developer.android.com/reference/android/hardware/camera/package-summary) package. 

> Note: If you aren't familiar with setting up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/).

> [Camera](https://developer.android.com/reference/android/hardware/camera/package-summary) class was deprecated in API level 21. Please check [Basic Video Capturer Camera 2](../Basic-Video-Capturer-Camera-2) project for most up to date solution. 

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

The `CustomVideoCapturer` class extends the `BaseVideoCapturer` class, defined in the OpenTok Android SDK.
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

The app calls `startCapture()` to start capturing video from the custom video capturer.

The class also implements the [PreviewCallback](https://developer.android.com/reference/android/hardware/Camera.PreviewCallback) interface. The `onPreviewFrame()` method of this interface is called as preview frames of the camera become available. In this method, the app calls the `provideByteArrayFrame()` method of the
`MirrorVideoCapturer` class (inherited from the `BaseVideoCapturer` class). This method
provides a video frame, defined as a byte array, to the video capturer:

```java
provideByteArrayFrame(data, NV21, mCaptureWidth, mCaptureHeight, currentRotation, isFrontCamera());
```

The publisher adds this video frame to the published stream.

## Further Reading

* Review Basic Video Capturer Camera 2](../Basic-Video-Capturer-Camera-2) project
* Review [Custom Video Driver](../Custom-Video-Driver) project
* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
