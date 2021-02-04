# Custom Video Driver

This app shows how to use both a custom video capturer and redender (custom video driver). While most applications will work fine with the default capturer (and therefore won't require an understanding of how the custom video driver work), if you need to add custom effects, then this is where you should start.
## Using a custom video capturer

After instantiating a Publisher object, the code sets a custom video capturer by calling the `setCapturer(BaseVideoCapturer capturer)` method of the Publisher:

```java
CustomVideoCapturer customVideoCapturer = new CustomVideoCapturer(
                    MainActivity.this,
                    Publisher.CameraCaptureResolution.MEDIUM,
                    Publisher.CameraCaptureFrameRate.FPS_30);
            
publisher = new Publisher.Builder(MainActivity.this)
        .capturer(customVideoCapturer)
        .renderer(new InvertedColorsVideoRenderer(MainActivity.this))
        .build();
```

The `CustomVideoCapturer` class extends the `BaseVideoCapturer` class, defined in the OpenTok Android SDK.
The `getCaptureSettings()` method returns the settings of the video capturer, including the frame
rate, width, height, video delay, and video format for the capturer:

```java
@Override
public synchronized CaptureSettings getCaptureSettings() {
    CaptureSettings settings = new CaptureSettings();
    settings.fps = desiredFps;
    settings.width = (null != cameraFrame) ? cameraFrame.getWidth() : 0;
    settings.height = (null != cameraFrame) ? cameraFrame.getHeight() : 0;
    settings.format = BaseVideoCapturer.NV21;
    settings.expectedDelay = 0;
    return settings;
}
```

The app calls `startCapture()` to start capturing video from the custom video capturer.

The class also implements the `android.hardware.Camera.PreviewCallback` interface. The
`onPreviewFrame()` method of this interface is called as preview frames of the camera become
available. In this method, the app calls the `provideByteArrayFrame()` method of the
`CustomVideoCapturer` class (inherited from the `BaseVideoCapturer` class). This method
provides a video frame, defined as a byte array, to the video capturer:

```java
provideByteArrayFrame(data, NV21, captureWidth, captureHeight, currentRotation, isFrontCamera());
```

The publisher adds this video frame to the published stream.

## Using a custom video renderer

You can use a custom video renderer for publisher and
subscriber videos. In this sample we will use a custom video renderer which inverts all colors
in the image.

After instantiating a Publisher object, the code sets a custom video renderer by calling the `setRenderer(BaseVideoRenderer renderer)` method of the Publisher:

```java
publisher = new Publisher.Builder(MainActivity.this)
        .capturer(customVideoCapturer)
        .renderer(new InvertedColorsVideoRenderer(MainActivity.this))
        .build();
```

The `InvertedColorsVideoRenderer` extends the `BaseVideoRenderer` class, defined in the OpenTok Android SDK.
The `InvertedColorsVideoRenderer` class includes a `MyRenderer` subclass that implements `GLSurfaceView.Renderer`.
This class includes a `displayFrame()` method that renders a frame of video to an Android view.

The `InvertedColorsVideoRenderer` constructor sets a property to an instance of the `MyRenderer` class.

```java
mRenderer = new MyRenderer();
```

The `onFrame()` method of the video renderer is inherited from the `BaseVideoRenderer` class.
This method is called at the specified frame rate. It then calls the `displayFrame()` method of
the M`yVideoRenderer` instance:

```java
@Override
public void onFrame(Frame frame) {
    renderer.displayFrame(frame);
    view.requestRender();
}
```

To render the video frames, the renderer class uses OpenGL shaders. In the sample we tweak the
shader to produce the inverted color effect, more precisely this is achieved by this line which is
inside the `fragmentShaderCode` String.

```java
"y=1.0-1.1643*(y-0.0625);\n"
```

## Further Reading

* Review [other sample projects](../)
* Review [Basic-Video-Capturer](../Basic-Video-Capturer)
* Review [Basic-Video-Renderer](../Basic-Video-Renderer)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
