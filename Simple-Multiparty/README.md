# Simple Multiparty

This app shows how to implement a simple video call application with several clients. It demonstrates how to enable/disable audio and video as well as swap the camera.

## Subcribing to multiple streams

[Signaling sample](../Signaling) subscribed to only one stream. In a multiparty video audio call
there are multiple streams.

```java
@Override
public void onStreamReceived(Session session, Stream stream) {
    final Subscriber subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
    session.subscribe(subscriber);
    addSubscriber(subscriber);
}
```

This simple multiparty app is able to handle a maximum of four subscribers. Once a
new stream is received, The `MainActivity` class creates a new `Subscriber` object and
subscribes the `Session` object to it. The Subscriber stream is then rendered to the
screen (as it did before).

## Adding user interface controls

This sample shows how you can add user interface controls for the following:
* Turning a publisher's audio stream on and off
* Swapping the publisher's camera

When the user taps the enable/disable audio button for the publisher, the following method of The `MainActivity` class
is invoked:

```java
toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (publisher == null) {
            return;
        }
        if (isChecked) {
            publisher.setPublishAudio(true);
        } else {
            publisher.setPublishAudio(false);
        }
    }
});
```

The `setPublishAudio(boolean publishAudio)` method of a `Publisher` object toggles its audio on or off, based on a
`Boolean` parameter.

When the user taps the enable/disable video button for the publisher, the following method of The `MainActivity` class
is invoked:

```java
toggleVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (publisher == null) {
            return;
        }
        if (isChecked) {
            publisher.setPublishVideo(true);
        } else {
            publisher.setPublishVideo(false);
        }
    }
});
```

The `setPublishVideo(boolean publishAudio)` method of a `Publisher` object toggles its video on or off, based on a
`Boolean` parameter.

When the user taps the swapCamera button, the following method of the OpenTokUI class
is invoked:

```java
swapCamera.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        if (publisher == null) {
            return;
        }

        publisher.cycleCamera();
    }
});
```

The `swapCamera` method of a Publisher object changes the camera used to the next available camera
on the device (if there is one).

Note: For the sake of simplicity, we have set a maximum of 4 subscribers for this application.

See below: 
```java
private final int MAX_NUM_SUBSCRIBERS = 4;
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
