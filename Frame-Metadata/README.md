# Frame Meta Data

This app shows how to send\retrieve additional metadata associated with each video frame.
## Using a custom video capturer

You can use a custom video capturer for a publisher. After
instantiating a Publisher object, the code sets a custom video capturer by calling the
`setCapturer(BaseVideoCapturer capturer)` method of the Publisher:

```java
publisher = new Publisher.Builder(MainActivity.this)
    .capturer(capturer)
    .renderer(renderer).build();
```

## Send frame metadata

The `MirrorVideoCapturer`  class extends the `BaseVideoCapturer` class, defined in the OpenTok Android SDK.
The `setCustomVideoCapturerDataSource` method provided metadata to be send with each frame (frame
rate, width, height, video delay, and video format for the capturer):

```java
MirrorVideoCapturer capturer = new MirrorVideoCapturer(
                    MainActivity.this,
                    Publisher.CameraCaptureResolution.MEDIUM,
                    Publisher.CameraCaptureFrameRate.FPS_30);

capturer.setCustomVideoCapturerDataSource(new MirrorVideoCapturer.CustomVideoCapturerDataSource() {
    // metadata to be send
    @Override
    public byte[] retrieveMetadata() {
        return getCurrentTimeStamp().getBytes();
    }
});
```

Above metadata is send inside `MirrorVideoCapturer.onPreviewFrame()` method:

```java
if (metadataSource != null) {
    byte[] framemetadata = metadataSource.retrieveMetadata();

    provideByteArrayFrame(data,
            NV21,
            captureWidth,
            captureHeight,
            currentRotation,
            isFrontCamera(),
            framemetadata);
}
```

## Receive frame metadata

The `InvertedColorsVideoRenderer` class extends the `BaseVideoRenderer` class, defined in the OpenTok Android SDK.
The `setInvertedColorsVideoRendererMetadataListener` method allows to retrieve incomming metadata:

```java
InvertedColorsVideoRenderer renderer = new InvertedColorsVideoRenderer(MainActivity.this);

renderer.setInvertedColorsVideoRendererMetadataListener(new InvertedColorsVideoRenderer.InvertedColorsVideoRendererMetadataListener() {
    // Retrieved metadata
    @Override
    public void onMetadataReady(byte[] metadata) {
        String timestamp = null;
        try {
            timestamp = new String(metadata, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println(timestamp);
    }
});
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
