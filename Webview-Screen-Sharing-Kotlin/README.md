# Webview Screen Sharing

This app shows how to use `WebView` as the source for screen-sharing video.

> Check [Basic-Video-Capturer-Camera-2](../Basic-Video-Capturer-Camera-2) project to see how a device camera can be used as the video source for the custom `Capturer`.
## Webview Screen sharing

Custom video capturer is using `WebView` from the Android application as the source of
a published stream.

When the app starts up, the `onCreate` method instantiates a `WebView` object:

```java
webViewContainer = findViewById(R.id.webview);
```

Upon connecting to the OpenTok session, the app instantiates a `Publisher` object, and calls its
`setCapturer` method to use a custom video capturer, defined by the `ScreenSharingCapturer`
class:

```kotlin
override fun onConnected(session: Session) {
    Log.d(TAG, "onConnected: Connected to session: ${session.sessionId}")

    val screenSharingCapturer = ScreenSharingCapturer(this@MainActivity, webViewContainer)

    publisher = Publisher.Builder(this@MainActivity).capturer(screenSharingCapturer).build()
    publisher?.setPublisherListener(publisherListener)
    publisher?.publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen

    webViewContainer.setWebViewClient(WebViewClient())
    val webSettings: WebSettings = webViewContainer.getSettings()
    webSettings.javaScriptEnabled = true
    webViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    webViewContainer.loadUrl("https://www.tokbox.com")
    
    publisher?.renderer?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
    publisherViewContainer.addView(publisher?.view)
    if (publisher?.view is GLSurfaceView) {
        (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
    }
    session.publish(publisher)
}
```

> Note: that the call to the `setPublisherVideoType` method sets the video type of the published
stream to `PublisherKitVideoType.PublisherKitVideoTypeScreen`. This optimizes the video encoding for
screen sharing. It is recommended to use a low frame rate (15 frames per second or lower) with this
video type. When using the screen video type in a session that uses the [OpenTok Media
Server](https://tokbox.com/opentok/tutorials/create-session/#media-mode), the
audio-only fallback feature is disabled, so that the video does not drop out in subscribers.

The `onConnected` method also calls the `loadScreenWebView` method. This method
configures the WebView object, loading the TokBox URL.

Note that the `webViewContainer` object is passed into the `ScreenSharingCapturer` constructor,
which assigns it to the `contentView` property. 

The `getCaptureSettings` method initializes capture settings to be used by the custom
video capturer:

```kotlin
override fun getCaptureSettings(): CaptureSettings {
    val captureSettings = CaptureSettings()
    captureSettings.fps = fps
    captureSettings.width = width
    captureSettings.height = height
    captureSettings.format = ARGB
    return captureSettings
}
```

The `startCapture` method starts the `frameProducer` thread after 1/15 second:

```kotlin
override fun startCapture(): Int {
    capturing = true
    handler.postDelayed(newFrame, (1000 / fps).toLong())
    return 0
}
```

The `frameProducer` thread gets a `Bitmap` representation of the `contentView` object
    (the `WebView`), writes its pixels to a buffer, and then calls the `provideIntArrayFrame()`
    method, passing in that buffer as a parameter:

```kotlin
private val newFrame: Runnable = object : Runnable {
    override fun run() {
        if (capturing) {
            val width = contentView.width
            val height = contentView.height
            if (frame == null || this@ScreenSharingCapturer.width != width || this@ScreenSharingCapturer.height != height) {
                this@ScreenSharingCapturer.width = width
                this@ScreenSharingCapturer.height = height
                if (bmp != null) {
                    bmp!!.recycle()
                    bmp = null
                }
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bmp!!)
                frame = IntArray(width * height)
            }
            canvas!!.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas!!.translate(-contentView.scrollX.toFloat(), -contentView.scrollY.toFloat())
            contentView.draw(canvas)
            bmp!!.getPixels(frame, 0, width, 0, 0, width, height)
            provideIntArrayFrame(frame, ARGB, width, height, 0, false)
            canvas!!.restore()
            handler.postDelayed(this, (1000 / fps).toLong())
        }
    }
}
```

The `provideIntArrayFrame` method, defined by the `BaseVideoCapturer` class sends an integer array of data to the publisher, to be used for the next video frame published.

If the publisher is still capturing video, the thread starts again after another 1/15 of a
second, so that the capturer continues to supply the publisher with new video frames to publish.
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
