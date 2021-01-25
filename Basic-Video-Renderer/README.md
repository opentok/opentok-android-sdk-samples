# Custom Video Renderer

This app shows how to use renderer. 

> Note: Check [Custom Video Driver](../Custom-Video-Driver) project to see how custom capturer and custom renderer works together.

The `MainActivity` class shows how you can use a custom video renderer for publisher and
subscriber videos. In this sample we will use a custom video renderer which inverts all colors
in the image.

After instantiating a Publisher object, the code sets a custom video renderer by calling the `setRenderer(BaseVideoRenderer renderer)` method of the Publisher:

```java
mPublisher = new Publisher(MainActivity.this, "publisher");
mPublisher.setPublisherListener(this);
mPublisher.setRenderer(new InvertedColorsVideoRenderer(this));
```

The `InvertedColorsVideoRenderer` class is defined in the `com.opentok.android.samples.customvideodriver`
package. This class extends the `BaseVideoRenderer` class, defined in the OpenTok Android SDK.
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
public void onFrame(Frame frame) {
    mRenderer.displayFrame(frame);
    mView.requestRender();
}
```

To render the video frames, the renderer class uses OpenGL shaders. In the sample we tweak the
shader to produce the inverted color effect, more precisely this is achieved by this line which is
inside the `fragmentShaderCode` String:

```java
"y=1.0-1.1643*(y-0.0625);\n" // this line produces the inverted effect
```

## Further Reading

* Review [Custom Video Driver](../Custom-Video-Driver) projcet
* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
