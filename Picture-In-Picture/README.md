# Picture In Picture

This app shows how to use the “Picture-in-Picture” (PiP) mode to be able to continue seeing the video of an OpenTok session while navigating between apps or browsing content on the main screen of your phone.

> Note: You need to have a minimum API level 26 to run this sample app.
## Using picture-in-picture

Update UI when Activity changes to and from picture-in-picture mode:

```java
@Override
public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

    if (isInPictureInPictureMode) {
        findViewById(R.id.button).setVisibility(View.GONE);
        publisherViewContainer.setVisibility(View.GONE);
        publisher.getView().setVisibility(View.GONE);
        getActionBar().hide();
    } else {
        findViewById(R.id.button).setVisibility(View.VISIBLE);
        publisherViewContainer.setVisibility(View.VISIBLE);
        publisher.getView().setVisibility(View.VISIBLE);
        if (publisher.getView() instanceof GLSurfaceView) {
            ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
        }
        getActionBar().show();
    }
}
```

Handler for button that enables picture in picture:

```java
public void pipActivity(View view) {
    PictureInPictureParams params = new PictureInPictureParams.Builder()
            .setAspectRatio(new Rational(9, 16)) // Portrait Aspect Ratio
            .build();
    
    enterPictureInPictureMode(params);
}
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
