# Texture View Renderer

In this sample we include a custom video renderer based on `android.view.TextureView` class.

When it comes to video rendering, there are different alternatives, in the OpenTok SDK, we support two ways of doing it:

1. Using a GLSurfaceView
2. Using a TextureView

Both classes, although both are very similar they have some differences that make them more appropriate according to the use case.
When using GLSurfaceView, it is more complicated to do some view compositions like applying a mask or doing animations with the views, 
on the other hand, when using TextureViews, these kind of things are feasible.

By default, OpenTok SDK uses a `GLSurfaceView` to render the video, but you can enable using a `TextureView` when creating a session:

```java
Session builder = new Session.Builder(this, apiKey, sessionId)
    .sessionOptions(new Session.SessionOptions() {
        @Override
        public boolean useTextureViews() {
            return true;
        }
    }).build();
``` 

In this sample we show how a `TextureView` renderer can be done and how it can be used when rendering video.

## TextureView based renderer

This renderer is an implementation of the OpenTok's `BaseVideoRenderer` and lives in `TextureViewRenderer` class of the sample.

`TextureViewRenderer` uses a `TextureView` android class in order to do the video rendering.

The schema of the `TextureViewRenderer` class is quite simple:
- When frames are received in the `BaseVideoRenderer.onFrame` method, it is saved in a local variable protected by a lock.
- `TextureView` render thread, takes the frame set by the previous method, and render it using OpenGL fragment and vertex shaders.

Working with OpenGL in Android can be messy given the different versions of OpenGL ES and different Operating system version support.
To make it easy we have imported some handy classes from Google's [grafika project](https://github.com/google/grafika). 
Those classes live in `com.android.grafika` package.

## Using a custom video renderer

To use the renderer we have created, we have to set it when building the `Publisher` or the `Renderer`

```java
   mPublisher = new Publisher(MainActivity.this, "publisher");
   mPublisher.setPublisherListener(this);
   mPublisher.setRenderer(new TextureViewRenderer(this));
```

## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
