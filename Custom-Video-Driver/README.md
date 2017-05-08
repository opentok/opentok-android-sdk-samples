# Project 2: Custom Video Driver

Note: Read the README.md file in the Project 1 folder before starting here.

## Using a custom video capturer

The MainActivity class shows how you can use a custom video capturer for a publisher. After
instantiating a Publisher object, the code sets a custom video capturer by calling the
`setCapturer(BaseVideoCapturer capturer)` method of the Publisher:

```java
    mPublisher = new Publisher(MainActivity.this, "publisher");
    mPublisher.setPublisherListener(this);
    mPublisher.setCapturer(new CustomVideoCapturer(MainActivity.this));
```

The CustomVideoCapturer class is defined in the `com.opentok.android.samples.custom_video_driver` package.
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

The app calls `startCapture()` to start capturing video from the custom video capturer.

The class also implements the android.hardware.Camera.PreviewCallback interface. The
onPreviewFrame() method of this interface is called as preview frames of the camera become
available. In this method, the app calls the provideByteArrayFrame() method of the
CustomVideoCapturer class (inherited from the BaseVideoCapturer class). This method
provides a video frame, defined as a byte array, to the video capturer:

```java
    provideByteArrayFrame(data, NV21, mCaptureWidth,
            mCaptureHeight, currentRotation, isFrontCamera());
```

The publisher adds this video frame to the published stream.

## Using a custom video renderer

The MainActivity class shows how you can use a custom video renderer for publisher and
subscriber videos. In this sample we will use a custom video renderer which inverts all colors
in the image.

After instantiating a Publisher object, the code sets a custom video renderer by calling the 'setRenderer(BaseVideoRenderer renderer)' method of the Publisher:

```java
   mPublisher = new Publisher(MainActivity.this, "publisher");
   mPublisher.setPublisherListener(this);
   mPublisher.setRenderer(new InvertedColorsVideoRenderer(this));
```

The InvertedColorsVideoRenderer class is defined in the `com.opentok.android.samples.custom_video_driver`
package. This class extends the BaseVideoRenderer class, defined in the OpenTok Android SDK.
The InvertedColorsVideoRenderer class includes a MyRenderer subclass that implements GLSurfaceView.Renderer.
This class includes a `displayFrame()` method that renders a frame of video to an Android view.

The InvertedColorsVideoRenderer constructor sets a property to an instance of the MyRenderer class.

```java
    mRenderer = new MyRenderer();
```

The `onFrame()` method of the video renderer is inherited from the BaseVideoRenderer class.
This method is called at the specified frame rate. It then calls the `displayFrame()` method of
the MyVideoRenderer instance:

```java
    public void onFrame(Frame frame) {
        mRenderer.displayFrame(frame);
        mView.requestRender();
    }
```

To render the video frames the renderer class uses OpenGL shaders. In the sample we tweak the
shader to produce the inverted color effect, more precisely this is achieved by this line which is
inside the `fragmentShaderCode` String.

```java
"y=1.0-1.1643*(y-0.0625);\n" // this line produces the inverted effect
```

## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
