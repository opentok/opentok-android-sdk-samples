# Live Photo Capture

This app shows how to capture an image from a subscribed video stream.
## Taking a screenshot of a subscribed video

When the app user clicks "Screenshot" in the main menu, the app starts an activity
defined by The `MainActivity` class. Upon connecting to the OpenTok session, the
activity publishes a stream. Upon the stream being created in the session, it
calls the `susbscribeToStream(stream)` method, which instantiates a Subscriber object and
sets its video renderer to a `BasicCustomVideoRenderer` object:

```java
subscriber = new Subscriber.Builder(this, stream)
    .renderer(new BasicCustomVideoRenderer(this))
    .build();
```

The `BasicCustomVideoRenderer` class extends the `BaseVideoRenderer` class defined in the OpenTok Android SDK. It contains a Boolean
`saveScreenshot` property (which is `false` by default).

When the user clicks the screenshot (camera) icon at the top of the user interface, the app calls
the `saveScreenshot(enableScreenshot)` method of the `BasicCustomVideoRenderer` instance, passing in
`true`:

```java
public void saveScreenshot(Boolean enableScreenshot) {
    saveScreenshot = enableScreenshot;
}
```

After that, when the subscriber receives a stream and the `displayFrame(Frame frame)` of the
`BasicCustomVideoRenderer` instance is called. It converts the frame into a `Bitmap` object, and then
saves that bitmap as a PNG file to the external storage directory for the app:

```java
public void displayFrame(Frame frame) {
frameLock.lock();

if (this.currentFrame != null) {
    this.currentFrame.recycle();
}

this.currentFrame = frame;
frameLock.unlock();

if (customVideoRenderer.saveScreenshot) {
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

    customVideoRenderer.saveScreenshot = false;
}
```

## Further Reading

Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
