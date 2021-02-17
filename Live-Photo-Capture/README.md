# Live Photo Capture

This app shows how to capture an image from a subscribed video stream.
## Taking a screenshot of a subscribed video

The code sets a custom video renderer by calling the `renderer` method inside `onStreamReceived` method:

```java
subscriber = new Subscriber.Builder(this, stream)
    .renderer(new ScreenshotVideoRenderer(this))
    .build();
```

The `ScreenshotVideoRenderer` class extends the `BaseVideoRenderer` class defined in the OpenTok Android SDK. It contains a Boolean `saveScreenshot` property (which is `false` by default).

When the app user clicks `Take Screenshot` button, the app calls
the `saveScreenshot()` method of the `ScreenshotVideoRenderer` instance:

```java
((ScreenshotVideoRenderer) subscriber.getRenderer()).saveScreenshot();
```

This method under the hood changes `saveScreenshot` property value to `true` in the `ScreenshotVideoRenderer.MyRenderer` class:

```java
public void saveScreenshot() {
    saveScreenshot = true;
}
```

If property `saveScreenshot` has value `true` during `displayFrame` method call then image file is saved to the root of device stoage:

```java
public void displayFrame(Frame frame) {
    frameLock.lock();

    this.currentFrame = frame;
    frameLock.unlock();

    if (saveScreenshot) {
        Log.d(TAG, "Screenshot capture");

        ByteBuffer bb = frame.getBuffer();
        bb.clear();

        int width = frame.getWidth();
        int height = frame.getHeight();
        int half_width = (width + 1) >> 1;
        int half_height = (height + 1) >> 1;
        int y_size = width * height;
        int uv_size = half_width * half_height;

        byte[] yuv = new byte[y_size + uv_size * 2];
        bb.get(yuv);
        int[] intArray = new int[width * height];

        // Decode Yuv data to integer array
        decodeYUV420(intArray, yuv, width, height);

        // Initialize the bitmap, with the replaced color
        Bitmap bmp = Bitmap.createBitmap(intArray, width, height, Bitmap.Config.ARGB_8888);

        try {
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOutputStream = null;
            File file = new File(path, "opentok-capture-" + System.currentTimeMillis() + ".png");
            fOutputStream = new FileOutputStream(file);

            bmp.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream);

            fOutputStream.flush();
            fOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        saveScreenshot = false;
    }
}
```

## Further Reading

- Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
