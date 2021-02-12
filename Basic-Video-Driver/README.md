# Basic Video Driver

This app shows how to use both a custom video capturer and redender together (custom video driver). While most applications will work fine with the default capturer (and therefore won't require an understanding of how the custom video driver work), if you need to add custom effects, then this is where you should start.
## Using a custom video capturer

After instantiating a Publisher object, the code sets a custom video capturer by calling the `setCapturer(BaseVideoCapturer capturer)` method of the Publisher:

```java
MirrorVideoCapturer mirrorVideoCapturer = new MirrorVideoCapturer(MainActivity.this);

publisher = new Publisher.Builder(MainActivity.this)
        .capturer(mirrorVideoCapturer)
        .build();
```

To learn more about `MirrorVideoCapturer` check [Basic-Video-Capturer-Camera-2](../Basic-Video-Capturer-Camera-2) project.

## Using a custom video renderer

You can use a custom video renderer for publisher and subscriber videos. In this sample we will use a custom video renderer which inverts colors in the video stream.

After instantiating a Publisher object, the code sets a custom video renderer by calling the `renderer` method of the Publisher:

```java
InvertedColorsVideoRenderer invertedColorsVideoRenderer = new InvertedColorsVideoRenderer(MainActivity.this);
            
publisher = new Publisher.Builder(MainActivity.this)
        .renderer(invertedColorsVideoRenderer)
        .build();
```

To learn more about `InvertedColorsVideoRenderer` check [Basic-Video-Renderer](../Basic-Video-Renderer) project.

## Further Reading

* Review [other sample projects](../)
* Review [Basic-Video-Capturer](../Basic-Video-Capturer)
* Review [Basic-Video-Renderer](../Basic-Video-Renderer)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
