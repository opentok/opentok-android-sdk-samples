# Simple Multiparty

Note: Read the README.md file in the Basic-Video-Chat folder before starting here.

## Subcribing to multiple streams

Previous samples subscribe to only one stream. In a multiparty video audio call
there should be multiple parties.

```java
@Override
public void onStreamReceived(Session session, Stream stream) {
    Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

    final Subscriber subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
    mSession.subscribe(subscriber);
    addSubscriber(subscriber);
}
```

This simple multiparty app is able to handle only four subsriber parties. On a
new stream received the MainActivity class creates a new Subscriber object and
subscribes the Session object to it. The Subscriber stream is rendered in the
screen as we did it before.

## Adding user interface controls

The MainActivity class shows how you can add user interface controls for the following:

* Turning a publisher's audio stream on and off
* Swapping the publisher's camera

The user interface is defined in the com.opentok.android.samples.simple_multiparty package.

When the user taps the mute button for the publisher, the following method of the MainActivity class
is invoked:

```java
toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mPublisher == null) {
            return;
        }
        if (isChecked) {
            mPublisher.setPublishAudio(true);
        } else {
            mPublisher.setPublishAudio(false);
        }
    }
});
```

The `setPublishAudio(boolean publishAudio)` method of a Publisher object toggles its audio on or off, based on a
Boolean parameter.

When the user taps the swapCamera button, the following method of the OpenTokUI class
is invoked:

```java
swapCamera.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        if (mPublisher == null) {
            return;
        }
        mPublisher.swapCamera();
    }
});
```

The `swapCamera()` method of a Publisher object changes the camera used to the next available camera
on the device (if there is one).

Note: For the sake of simplicity, we have set a maximum of 4 subscribers for this application.

See below: 
```java
private final int MAX_NUM_SUBSCRIBERS = 4;
```

## Next steps

For details on the full OpenTok Android API, see the [reference
documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).
