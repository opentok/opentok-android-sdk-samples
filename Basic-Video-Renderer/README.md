# Custom Video Renderer

This app shows how you can use a custom video renderer for publisher and
subscriber videos. In this sample app we will use a custom video renderer which inverts image colors.

The code sets a custom video renderer by calling the `setRenderer()` method of the Publisher:

```java
publisher = new Publisher.Builder(MainActivity.this)
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
inside the `fragmentShaderCode` String:

```java
"y=1.0-1.1643*(y-0.0625);\n"
```

## Further Reading

* Review [Custom Video Driver](../Custom-Video-Driver) projcet
* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
