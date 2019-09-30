# TokBox integration with Android ARCore
This sample code will help you to better understand in integrating `opentok` Android SDK with `ARCore` from Google. To make this sample code running, you need to fill up some information inside `AugmentedFaceActivity`.

Please fill `TOKEN`, `SESSION_ID` and `API_KEY`

This sample code is using custom video driver. The custom video drive sample code can be found inside `CustomVideoCapturer.java`. In short, we use this simple code to copy canvas from the view to stream it inside the video capturer
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
