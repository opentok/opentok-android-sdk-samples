Video Transformers
======================

The Video Transformers app is a very simple application created on top of Basic Video Chat meant to get a new developer
started using Media Processor APIs on OpenTok Android SDK. For a full description, see the [Video Transformers tutorial at the
OpenTok developer center](https://tokbox.com/developer/guides/vonage-media-processor/android).

You can use pre-built transformers in the Vonage Media Processor library or create your own custom video transformer to apply to published video.

You can use the <a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.html#setAudioTransformers(java.util.ArrayList)"><code>PublisherKit.setAudioTransformers()</code></a> and
<a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.html#setVideoTransformers(java.util.ArrayList)"><code>PublisherKit.setVideoTransformers()</code></a>
methods to apply audio and video transformers to a stream.

For video, you can apply the background blur video transformer included in the Vonage Media Library.

You can also create your own custom audio and video transformers.

## Applying a video transformer from the Vonage Media Library

Use the <a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.VideoTransformer.html#%3Cinit%3E(java.lang.String,java.lang.String)"><code>PublisherKit.VideoTransformer(String name, String properties)</code></a>
constructor to create a video transformer that uses a named transformer from the Vonage Media Library.

Currently, only one transformer is supported: background blur. Set the `name` parameter to `"BackgroundBlur"`.
Set the `properties` parameter to a JSON string defining properties for the transformer.
For the background blur transformer, this JSON includes one property -- `radius` -- which can be set
to `"High"`, `"Low"`, or `"None"`.

```java
ArrayList<PublisherKit.VideoTransformer> videoTransformers = new ArrayList<>();
PublisherKit.VideoTransformer backgroundBlur = new PublisherKit.VideoTransformer(
    "BackgroundBlur",
    "{\"radius\":\"High\"}"
);
videoTransformers.add(backgroundBlur);
mPublisher.setVideoTransformers(videoTransformers);
```

## Creating a custom video transformer

Create a class that implements the <a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.CustomVideoTransformer.html"><code>PublisherKit.CustomVideoTransformer</code></a> 
interface. Implement the `PublisherKit.CustomVideoTransformer.onTransform​(BaseVideoRenderer.Frame frame)` method. The `PublisherKit.CustomVideoTransformer.onTransform​(BaseVideoRenderer.Frame frame)` method is triggered for each video frame.
In the implementation of the method, apply a transformation to the `frame` object passed into the method:

```java
public class MyCustomTransformer implements PublisherKit.CustomVideoTransformer {
    @Override
    public void onTransform(BaseVideoRenderer.Frame frame) {
        // Replace this with code to transform the frame:
        frame.convertInPlace(frame.getYplane(), frame.getVplane(), frame.getUplane(), frame.getYstride(), frame.getUvStride());
    }
}
```

Then pass the object that implements the PublisherKit.CustomVideoTransformer interface into the `PublisherKit.setVideoTransformers()` method:

```java
MyCustomTransformer transformer = new MyCustomTransformer();
ArrayList<PublisherKit.VideoTransformer> videoTransformers = new ArrayList<>();
videoTransformers.add(transformer);
mPublisher.setVideoTransformers(videoTransformers);
```

You can combine the Vonage Media library transformer (see the previous section) with custom transformers or apply
multiple custom transformers by adding multiple PublisherKit.VideoTransformer objects to the ArrayList passed
into the `PublisherKit.setVideoTransformers()` method.

Then pass the object that implements the PublisherKit.CustomAudioRransformer interface into the `PublisherKit.setAudioTransformers()` method:

```java
MyCustomAudioTransformer transformer = new MyCustomAudioTransformer();
ArrayList<PublisherKit.VideoTransformer> audioTransformers = new ArrayList<>();
audioTransformers.add(transformer);
mPublisher.setAudioTransformers(audioTransformers);
```

You can apply multiple custom transformers by adding multiple PublisherKit.AudioTransformer objects to the ArrayList
passed into the `PublisherKit.setAudioTransformers()` method.

## Clearing video transformers for a publisher

To clear video transformers for a publisher, pass an empty ArrayList into
into the `PublisherKit.setVideoTransformers()` method.

```java
videoTransformers.clear();
mPublisher.setVideoTransformers(videoTransformers);
```
