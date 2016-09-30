# Project 4: Screen Sharing

Note: Read the README.md file in the Project 1 folder before starting here.

## Screen sharing

You can use a custom video capturer to use a view from the Android application as the source of
a published stream.

When the app starts up, the `onCreate(Bundle savedInstanceState)` method instantiates a WebView
object:

```java
        mWebViewContainer = (WebView) findViewById(R.id.webview);
```

The app will use this WebView as the source for the publisher video (instead of a camera).

Upon connecting to the OpenTok session, the app instantiates a Publisher object, and calls its
`setCapturer()` method to use a custom video capturer, defined by the ScreensharingCapturer
class:

```java
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

        mPublisher = new Publisher(MainActivity.this, "publisher");
        mPublisher.setPublisherListener(this);
        mPublisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
        mPublisher.setAudioFallbackEnabled(false);

        ScreensharingCapturer screenCapturer = new ScreensharingCapturer(MainActivity.this, mWebViewContainer);
        mPublisher.setCapturer(screenCapturer);

        mWebViewContainer.setWebViewClient(new WebViewClient());
        WebSettings webSettings = mWebViewContainer.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mWebViewContainer.loadUrl("http://www.tokbox.com");

        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView());

        mSession.publish(mPublisher);
    }
```

Note that the call to the `setPublisherVideoType()` method sets the video type of the published
stream to `PublisherKitVideoType.PublisherKitVideoTypeScreen`. This optimizes the video encoding for
screen sharing. It is recommended to use a low frame rate (5 frames per second or lower) with this
video type. When using the screen video type in a session that uses the [OpenTok Media
Server](https://tokbox.com/opentok/tutorials/create-session/#media-mode), the
audio-only fallback feature is disabled, so that the video does not drop out in subscribers.

The `onConnected(Session session)` method also calls the `loadScreenWebView()` method. This method
configures the WebView object, loading the TokBox URL.

Note that the `mWebViewContainer` object is passed into the ScreensharingCapturer() constructor,
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
                    
                    bmp = Bitmap.createBitmap(width,
                            height, Bitmap.Config.ARGB_8888);
                    
                    canvas = new Canvas(bmp);
                    frame = new int[width * height];
                }
                
                contentView.draw(canvas);
                
                bmp.getPixels(frame, 0, width, 0, 0, width, height);
 
                provideIntArrayFrame(frame, ARGB, width, height, 0, false);
 
                mHandler.postDelayed(newFrame, 1000 / fps);
            }
        }
    };
```

## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
