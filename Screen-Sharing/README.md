# Screen Sharing

This app shows how to to publish a screen-sharing video, using the device screen as the source for the stream's video.

> Note: If you aren't familiar with setting up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/).

## Screen sharing

You can use a custom video capturer to use a view from the Android application as the source of
a published stream.

When the app starts up, the `onCreate(Bundle savedInstanceState)` method instantiates a WebView
object:

```java
mWebViewContainer = findViewById(R.id.webview);
```

The app will use this WebView as the source for the publisher video (instead of a camera).

Upon connecting to the OpenTok session, the app instantiates a Publisher object, and calls its
`setCapturer()` method to use a custom video capturer, defined by the ScreensharingCapturer
class:

```java
@Override
public void onConnected(Session session) {
    Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

    publisher = new Publisher(this, "publisher");
    publisher.setPublisherListener(publisherListener);
    publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
    publisher.setAudioFallbackEnabled(false);

    ScreensharingCapturer screenCapturer = new ScreensharingCapturer(this, mWebViewContainer);
    publisher.setCapturer(screenCapturer);

    mWebViewContainer.setWebViewClient(new WebViewClient());
    WebSettings webSettings = mWebViewContainer.getSettings();
    webSettings.setJavaScriptEnabled(true);
    mWebViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    mWebViewContainer.loadUrl("http://www.tokbox.com");

    publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
    publisherViewContainer.addView(publisher.getView());

    session.publish(publisher);
}
```

> Note: that the call to the `setPublisherVideoType()` method sets the video type of the published
stream to `PublisherKitVideoType.PublisherKitVideoTypeScreen`. This optimizes the video encoding for
screen sharing. It is recommended to use a low frame rate (5 frames per second or lower) with this
video type. When using the screen video type in a session that uses the [OpenTok Media
Server](https://tokbox.com/opentok/tutorials/create-session/#media-mode), the
audio-only fallback feature is disabled, so that the video does not drop out in subscribers.

The `onConnected(Session session)` method also calls the `loadScreenWebView()` method. This method
configures the WebView object, loading the TokBox URL.

Note that the `mWebViewContainer` object is passed into the `ScreensharingCapturer()` constructor,
which assigns it to the `contentView` property. The `newFrame()` method is called when the video
capturer supplies a new frame to the video stream. It creates a canvas, draws the `contentView`
to the canvas, and assigns the bitmap representation of `contentView` to the frame to be sent:

```java
Runnable newFrame = new Runnable() {
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

            mHandler.postDelayed(newFrame, 1000 / fps);
        }
    }
};
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
