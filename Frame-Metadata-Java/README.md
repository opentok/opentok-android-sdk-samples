# Frame Meta Data

This app shows how to send and receive metadata associated with each video frame.
## Using a custom video capturer

App uses a custom video capturer to send metadata and a custom video renderer to recieve metadata:

```java
publisher = new Publisher.Builder(MainActivity.this)
    .capturer(sendFrameMetaDataCapturer)
    .renderer(receiveFrameMetaDataRenderer)
    .build();
```

## Send frame metadata

The `SendFrameMetaDataCapturer` class extends the `BaseVideoCapturer` class, defined in the OpenTok Android SDK.
The `setCustomMetadataSource` method provides metadata to be send with each video frame:

```java
SendFrameMetaDataCapturer sendFrameMetaDataCapturer = new SendFrameMetaDataCapturer(MainActivity.this);

// metadata to be send
sendFrameMetaDataCapturer.setCustomMetadataSource(() -> {
    String timestamp = getCurrentTimeStamp();
    return timestamp.getBytes();
});
```

Under the hood above metadata is added to the frame inside `SendFrameMetaDataCapturer.onPreviewFrame` method:

```java
if (metadataSource != null) {
    byte[] frameMetadata = customMetadataSource.retrieveMetadata();

    provideByteArrayFrame(data,
            NV21,
            captureWidth,
            captureHeight,
            currentRotation,
            isFrontCamera(),
            frameMetadata);
}
```

## Receive frame metadata

The `ReceiveFrameMetaDataRenderer` class extends the `BaseVideoRenderer` class, defined in the OpenTok Android SDK.
The `setCustomMetadataListener` method allows to retrieve metadata from incomming video frames:

```java
ReceiveFrameMetaDataRenderer receiveFrameMetaDataRenderer = new ReceiveFrameMetaDataRenderer(MainActivity.this);

// Retrieved metadata
receiveFrameMetaDataRenderer.setCustomMetadataListener(metadata -> {
    String timestamp = null;

    try {
        timestamp = new String(metadata, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
    }

});
```

nther the hood above metadata is retrieved from the frame inside `ReceiveFrameMetaDataRenderer.MyRenderer.onDrawFrame` method:

```java
if (customMetadataListener != null) {
    customMetadataListener.onMetadataReady(currentFrame.getMetadata());
}
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
