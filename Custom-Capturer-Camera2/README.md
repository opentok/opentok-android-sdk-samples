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
                    .renderer(new InvertedColorsVideoRenderer(MainActivity.this)).build();
            mPublisher.setPublisherListener(this);
```

The CustomVideoCapturerCamera2 class is defined in the `com.opentok.android.samples.custom_capturer_camera2` package.
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

The publisher adds this video frame to the published stream.

## Using a custom video renderer

The MainActivity class shows how you can use a custom video renderer for publisher and
subscriber videos. In this sample we will use a custom video renderer which inverts all colors
in the image.

After instantiating a Publisher object, the code sets a custom video renderer by calling the 'setRenderer(BaseVideoRenderer renderer)' method of the Publisher:

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
