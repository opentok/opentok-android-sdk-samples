# Custom Video Renderer

This app shows how you can use a custom video renderer for publisher and
subscriber video streams. This custom video renderer which inverts the video colors.

The code sets a custom video renderer by calling the `renderer` method during `Subscriber` construction:

```kotlin
subscriber = Subscriber.Builder(this@MainActivity, stream)
    .renderer(InvertedColorsVideoRenderer(this@MainActivity))
    .build()
```

The code sets a custom video renderer by calling the `renderer` method during `Publisher` construction:

```kotlin
publisher = Publisher.Builder(this@MainActivity)
    .renderer(InvertedColorsVideoRenderer(this@MainActivity))
    .build()
```

The `InvertedColorsVideoRenderer` is a custom class that extends the `BaseVideoRenderer` class, defined in the Vonage Android SDK.

The `InvertedColorsVideoRenderer` constructor sets a `renderer` property to a `GLSurfaceView` object.
The app uses this object to display the video using OpenGL ES 2.0. The renderer for this
`GLSurfaceView` object is set to a `MyRenderer` object. `MyRenderer` is a custom class that
implements `GLSurfaceView.Renderer`, and it is used to render the video to the `GLSurfaceView`
object:

```kotlin
init {
    view = GLSurfaceView(context)
    view.setEGLContextClientVersion(2)

    renderer = MyRenderer()
    view.setRenderer(renderer)

    view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
}
```

The `onFrame` method of the video renderer is inherited from the `BaseVideoRenderer` class.
The `BaseVideoRenderer.onFrame` method is called when the publisher (or subscriber) renders
a video frame to the video renderer. The `InvertedColorsVideoRenderer` implementation of this method, it takes the frame's image buffer ([YUV](https://en.wikipedia.org/wiki/YUV) representation of the frame), passes it to the `displayFrame` method of the `MyRenderer` object and calls the `requestRender` method of the `GLSurfaceView` object:

```kotlin
override fun onFrame(frame: Frame) {
    renderer.displayFrame(frame)
    view.requestRender()
}
```

It is important to note that the frame that the sdk sends to the Renderer in the `onFrame` method is now property of the Renderer. It is up to this class to destroy the frame when it is not needed anymore.
That's why we destroy previous frame when a new one comes to the Renderer

```kotlin
fun displayFrame(frame: Frame) {
    frameLock.lock()

    currentFrame?.destroy() // Disposes previous frame
    
    currentFrame = frame
    frameLock.unlock()
}
```

To render the video frames, the renderer class uses `OpenGL` shaders. In this sample
shader produces the inverted color effect, more precisely this is achieved by this line which is
inside the `fragmentShaderCode` String:

```kotlin
"y=1.0-1.1643*(y-0.0625);\n" // this line produces the inverted effect
//   "y=1.1643*(y-0.0625);\n"  // use this line instead if you want to have normal colors
```

## Further Reading

* Review [Custom Video Driver](../Custom-Video-Driver) projcet
* Review [other sample projects](../)
* Read more about [Vonage Android SDK](https://developer.vonage.com/en/video/client-sdks/android/overview)