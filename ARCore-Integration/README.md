# ARCore Integration

This app shows how to integrate [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/) with [ARCore](https://developers.google.com/ar) from Google. 

This sample code is using a custom video capturer (`CustomVideoCapturer.java`). This code copies canvas from the view to the stream (it's like a screen sharing of the ARView):


```java
PixelCopy.request(contentView, bitmap, CustomVideoCapturer.this, handlerPixelCopy);

@Override
public void onPixelCopyFinished(int copyResult) {
    bitmap.getPixels(frame, 0, width, 0, 0, width, height);

    // this method will send the frame directly to stream
    provideIntArrayFrame(frame, ARGB, width, height, 0, false);
    handler.postDelayed(newFrame, 1000 / fps);
}
```

> Note: It may be possible to retrieve ARCore camera stream directly from ARCore SDK however more research is required (this may be a better approach because the view can be clipped by the layout, so only part of the original camera stream will be send)

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
* Read more about [ARCore](https://developers.google.com/ar)