# Screen Sharing

This app shows how to use `WebView` as the source for the publisher video (instead of a camera)..
## Screen sharing

You can use a custom video capturer to use a view from the Android application as the source of
a published stream.

When the app starts up, the `onCreate(Bundle savedInstanceState)` method instantiates a `WebView`
object:

```java
webViewContainer = findViewById(R.id.webview);
```

The app will

Upon connecting to the OpenTok session, the app instantiates a `Publisher` object, and calls its
`setCapturer` method to use a custom video capturer, defined by the `ScreensharingCapturer`
class:

```java
@Override
public void onConnected(Session session) {
    Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

    ScreensharingCapturer screenCapturer = new ScreensharingCapturer(MainActivity.this, webViewContainer);

    publisher = new Publisher.Builder(MainActivity.this)
            .capturer(screenCapturer)
            .build();
            
    publisher.setPublisherListener(publisherListener);
    publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
    publisher.setAudioFallbackEnabled(false);

    webViewContainer.setWebViewClient(new WebViewClient());
    WebSettings webSettings = webViewContainer.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    webViewContainer.loadUrl("https://www.tokbox.com");

    publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
    publisherViewContainer.addView(publisher.getView());

    session.publish(publisher);
}
```

> Note: that the call to the `setPublisherVideoType` method sets the video type of the published
stream to `PublisherKitVideoType.PublisherKitVideoTypeScreen`. This optimizes the video encoding for
screen sharing. It is recommended to use a low frame rate (5 frames per second or lower) with this
video type. When using the screen video type in a session that uses the [OpenTok Media
Server](https://tokbox.com/opentok/tutorials/create-session/#media-mode), the
audio-only fallback feature is disabled, so that the video does not drop out in subscribers.

The `onConnected(Session session)` method also calls the `loadScreenWebView` method. This method
configures the WebView object, loading the TokBox URL.

Note that the `webViewContainer` object is passed into the `ScreensharingCapturer()` constructor,
which assigns it to the `contentView` property. The `newFrame` method is called when the video
capturer supplies a new frame to the video stream. It creates a canvas, draws the `contentView`
to the canvas, and assigns the bitmap representation of `contentView` to the frame to be sent:

```java
private Runnable newFrame = new Runnable() {
    @Override
    public void run() {
        if (capturing) {
            int width = contentView.getWidth();
            int height = contentView.getHeight();

            if (frame == null ||
                    ScreensharingCapturer.this.width != width ||
                    ScreensharingCapturer.this.height != height) {

                ScreensharingCapturer.this.width = width;
                ScreensharingCapturer.this.height = height;

                if (bmp != null) {
                    bmp.recycle();
                    bmp = null;
                }

                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                canvas = new Canvas(bmp);
                frame = new int[width * height];
            }
            canvas.saveLayer(0, 0, width, height, null);
            canvas.translate(-contentView.getScrollX(), - contentView.getScrollY());
            contentView.draw(canvas);

            bmp.getPixels(frame, 0, width, 0, 0, width, height);

            provideIntArrayFrame(frame, ARGB, width, height, 0, false);

            canvas.restore();

            handler.postDelayed(newFrame, 1000 / fps);

        }
    }
};
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
