# ARCore Integration

This app shows how to integrate [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/) with [ARCore](https://developers.google.com/ar) from Google. It utilizes 
[Augmented Faces](https://developers.google.com/ar/develop/java/augmented-faces) to add overlays facial features of a fox onto a user's face using assets that include 3D models and an overlay texture.

This sample code is using a custom video capturer (`CustomVideoCapturer.java`). This code copies canvas from the view to the stream (it's like a screensharing of the ARView):


```java
PixelCopy.request((SurfaceView) mContentView, mBitmap, CustomVideoCapturer.this, handlerPixelCopy);

@Override
  public void onPixelCopyFinished(int copyResult) {
    mBitmap.getPixels(frame, 0, width, 0, 0, width, height);

    // this method will send the frame directly to stream
    provideIntArrayFrame(frame, ARGB, width, height, 0, false);
    mHandler.postDelayed(newFrame, 1000 / fps);
  }
```

> Note: It may be possible to retrieve ARCore camera stream directly from ARCore SDK however more research is required (this may be a better approach, because the view can 
be clipped by the layout, so only part of the orginal camera stream wil be send)

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
* Read more about [ARCore](https://developers.google.com/ar)