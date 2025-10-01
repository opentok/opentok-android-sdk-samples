# Screen Sharing

This app demonstrates how to use the Media Projection API as the source for screen-sharing video.

> Check [Basic-Video-Capturer-Camera-2](../Basic-Video-Capturer-Camera-2) project to see how a device camera can be used as the video source for the custom `Capturer`.

## Screen sharing

The custom video capturer uses the Media Projection API to capture the device's screen and publish it as a video stream.

When the app starts up, the `onCreate` method initializes the Media Projection API by requesting permission to capture the screen:

```java
mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
```

Upon connecting to the OpenTok session, the app instantiates a `Publisher` object and calls its `setCapturer` method to use a custom video capturer, defined by the `ScreenSharingCapturer` class:

```java
@Override
public void onConnected(Session session) {
    ScreenSharingCapturer screenSharingCapturer = new ScreenSharingCapturer(MainActivity.this, mediaProjection);

    publisher = new Publisher.Builder(MainActivity.this)
            .capturer(screenSharingCapturer)
            .build();
            
    publisher.setPublisherListener(publisherListener);
    publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
    publisher.setAudioFallbackEnabled(false);

    publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
    publisherViewContainer.addView(publisher.getView());

    session.publish(publisher);
}
```

> Note: The call to the `setPublisherVideoType` method sets the video type of the published stream to `PublisherKitVideoType.PublisherKitVideoTypeScreen`. This optimizes the video encoding for screen sharing. It is recommended to use a low frame rate (15 frames per second or lower) with this video type. When using the screen video type in a session that uses the [OpenTok Media Server](https://tokbox.com/opentok/tutorials/create-session/#media-mode), the audio-only fallback feature is disabled, so that the video does not drop out in subscribers.

The `ScreenSharingCapturer` class uses the Media Projection API to capture the screen. The `getCaptureSettings` method initializes capture settings to be used by the custom video capturer:

```java
@Override
public CaptureSettings getCaptureSettings() {

    CaptureSettings captureSettings = new CaptureSettings();
    captureSettings.fps = fps;
    captureSettings.width = width;
    captureSettings.height = height;
    captureSettings.format = ARGB;
    return captureSettings;
}
```

The `startCapture` method starts the screen capture process:

```java
@Override
public int startCapture() {
    capturing = true;

    virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenSharing",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            surface,
            null,
            null
    );

    handler.postDelayed(newFrame, 1000 / fps);
    return 0;
}
```

The `backgroundHandler` thread captures frames from the virtual display, processes them, and sends them to the publisher:

```java
imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();

                    if (frame == null) {
                        frame = new int[width * height];
                    }

                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int index = y * rowStride + x * pixelStride;
                            int pixel = buffer.getInt(index);
                            frame[y * width + x] = pixel;
                        }
                    }

                    provideIntArrayFrame(frame, ABGR, width, height, 0, false);
                    image.close();
                }
            }
        }, backgroundHandler);
```

The `provideIntArrayFrame` method, defined by the `BaseVideoCapturer` class, sends an integer array of data to the publisher, to be used for the next video frame published.

If the publisher is still capturing video, the thread starts again after another 1/15 of a second, so that the capturer continues to supply the publisher with new video frames to publish.

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
