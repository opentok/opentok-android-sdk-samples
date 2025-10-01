Media Transformers
======================

The Video Transformers app is a very simple application created on top of Basic Video Chat meant to get a new developer
started using Media Processor APIs on OpenTok Android SDK. For a full description, see the [Media Transformers tutorial at the
OpenTok developer center](https://tokbox.com/developer/guides/vonage-media-processor/android).

<p class="topic-summary">
You can use pre-built transformers in the Vonage Media Processor library or create your own custom audio
or video transformer to apply to published video.
</p>

You can use the <a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.html#setAudioTransformers(java.util.ArrayList)"><code>PublisherKit.setAudioTransformers()</code></a> and
<a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.html#setVideoTransformers(java.util.ArrayList)"><code>PublisherKit.setVideoTransformers()</code></a>
methods to apply audio and video transformers to a stream.

<div class="important">
  <b>Important:</b>
  <p>
    Media transformations are not supported on all devices. See <a href="#client-requirements">Client requirements</a>.
  </p>
</div>

The Vonage Video Android SDK includes two ways to implement transformers:

* **Moderate** — For video, you can apply the background blur and background replacement video transformers included in the [Vonage Media Library](#vonage-media-library-integration). See [Applying a video transformer from the Vonage Media Library](#applying-a-video-transformer-from-the-vonage-media-library). For audio, you can apply the noise suppression audio transformer included in the [Vonage Media Library](#vonage-media-library-integration). See [Applying an audio transformer from the Vonage Media Library](#applying-an-audio-transformer-from-the-vonage-media-library).

* **Advanced** — You can create your own [custom video transformers](#creating-a-custom-video-transformer) and [custom audio transformers](#creating-a-custom-audio-transformer).

<a name="supported-devices-and-android-versions"></a>
## Client requirements

The transformers from the Vonage Media Library are supported on Android API Level of 23 or above, on the following devices:

* Samsung Galaxy S8 and above
* Google Pixel 5 and above
* OPPO A94 and above
* Android phones using Qualcomm Snapdragon 835 and above
* Android Phones using Qualcomm Snapdragon 765G and above

Test on other devices to check for support.

Transformers require adequate processor support. Even on supported devices, transformers may not be stable when background processes limit available processing resources. The same limitations may apply with custom media transformers in addition to transformers from the Vonage Media Library.

Android may throttle CPU performance to conserve energy (for example, to extend battery life) by throttling CPU performance. This may result in suboptimal transformer performance and introduce unwanted audio or video artifacts. We recommend disabling battery saver or low-power mode, if it is an option, in such cases.

Many video transformations (such as background blur) use segmentation to separate the speaker from the background. For best results, use proper lighting and a plain background. Insufficient lighting or complex backgrounds may cause video artifacts (for example, the speaker or a hat the speaker is wearing may get blurred along with the background).

You should perform benchmark tests on as many supported devices as possible, regardless of the transformation.

# Vonage Media Library integration

Due to significant increased size when integrating Vonage Media Library into SDK, from OpenTok SDK v2.27.2 the Media Transformers are available via the opt-in Vonage Media Library. This library needs to explicitly be added to the project. 

The Vonage Media Library was initially embedded in OpenTok SDK. If your OpenTok SDK version is older than 2.27.2, move directly to [Applying a video transformer from the Vonage Media Library](#applying-a-video-transformer-from-the-vonage-media-library) and [Applying an audio transformer from the Vonage Media Library](#applying-an-audio-transformer-from-the-vonage-media-library).

A Maven version is available at https://central.sonatype.com/artifact/com.vonage/client-sdk-video-transformers.
The artifact ID is `"client-sdk-video-transformers"`.

Modify the app's build.gradle file and add the following code snippet to the
`dependencies` section:

```
implementation 'com.vonage:client-sdk-video-transformers:2.27.2'
```

If a call to <code>PublisherKit.VideoTransformer(String name, String properties)</code> or <code>PublisherKit.AudioTransformer(String name, String properties)</code> is made without loading the library, the transformer returned will be null. An exception will be raised with the following error code `0x0A000006 - OTC_MEDIA_TRANSFORMER_OPENTOK_TRANSFORMERS_LIBRARY_NOT_LOADED`.

## Applying a video transformer from the Vonage Media Library

Use the <a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.VideoTransformer.html#%3Cinit%3E(java.lang.String,java.lang.String)"><code>PublisherKit.VideoTransformer(String name, String properties)</code></a>
constructor to create a video transformer that uses a named transformer from the Vonage Media Library.

Two transformers are supported:

* **Background blur.** For this filter, set the `name` parameter to `"BackgroundBlur"`. Set the `properties` parameter to a JSON string defining properties for the transformer. For the background blur transformer, this JSON includes one property -- `radius` -- which can be set
to `"High"`, `"Low"`, `"None"`, or `"Custom`. If you set the `radius` property to "Custom", add a `custom_radius` property to the JSON string: `"{\"radius\":\"Custom\",\"custom_radius\":\"value\"}"` (where `custom_radius` is a positive integer defining the blur radius).

  ```java
  Publisher publisher = new Publisher.Builder(MainActivity.this).build();
  ArrayList<PublisherKit.VideoTransformer> videoTransformers = new ArrayList<>();
  PublisherKit.VideoTransformer backgroundBlur = publisher.new VideoTransformer(
      "BackgroundBlur",
      "{\"radius\":\"High\"}"
  );
  videoTransformers.add(backgroundBlur);
  publisher.setVideoTransformers(videoTransformers);
  ```

* **Background replacement.** For this filter, set the `name` parameter to `"BackgroundReplacement"`. And set a `properties` parameter to a JSON string. The format of the JSON is `"{\"image_file_path\":\"path/to/image\"}"`, where `image_file_path` is the absolute file path of a local image to use as virtual background. Supported image formats are PNG and JPEG.

  ```java
  Publisher publisher = new Publisher.Builder(MainActivity.this).build();
  ArrayList<PublisherKit.VideoTransformer> videoTransformers = new ArrayList<>();
  PublisherKit.VideoTransformer backgroundReplacement = publisher.new VideoTransformer(
      "BackgroundReplacement",
      "{\"image_file_path\":\"path-to-image-file\"}"
  );
  videoTransformers.add(backgroundReplacement);
  publisher.setVideoTransformers(videoTransformers);
  ```

## Applying an audio transformer from the Vonage Media Library

<p class="note">
  <b>Note:</b> This is a beta feature.
</p> 

Use the <a href="http://localhost:9778/developer/sdks/android/reference/com/opentok/android/PublisherKit.AudioTransformer.html#<init>(java.lang.String,java.lang.String)"><code>PublisherKit.AudioTransformer(String name, String properties)</code></a>
constructor to create an audio transformer that uses a named transformer from the Vonage Media Library.

One transformer is supported:

* **Noise Suppression.** For this filter, set the `name` parameter to `"NoiseSuppression"`. Set the `properties` parameter to a JSON string defining properties for the transformer. For the noise suppression transformer, this JSON does not include any property at the moment. Set it to an empty JSON string `"{}"`.

  ```java
  Publisher publisher = new Publisher.Builder(MainActivity.this).build();
  ArrayList<PublisherKit.AudioTransformer> audioTransformers = new ArrayList<>();
  PublisherKit.AudioTransformer noiseSuppression = publisher.new AudioTransformer(
      "NoiseSuppression",
      "{}"
  );
  audioTransformers.add(noiseSuppression);
  publisher.setAudioTransformers(audioTransformers);
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
  Publisher publisher = new Publisher.Builder(MainActivity.this).build();
  MyCustomTransformer transformer = new MyCustomTransformer();
  PublisherKit.VideoTransformer myCustomTransformer = publisher.new VideoTransformer("myTransformer", transformer);
  ArrayList<PublisherKit.VideoTransformer> videoTransformers = new ArrayList<>();
  videoTransformers.add(myCustomTransformer);
  publisher.setVideoTransformers(videoTransformers);
```

You can combine the Vonage Media library transformer (see the previous section) with custom transformers or apply
multiple custom transformers by adding multiple PublisherKit.VideoTransformer objects to the ArrayList passed
into the `PublisherKit.setVideoTransformers()` method.

adding multiple OTPublisherKit.VideoTransformer objects to the array used
for the `OTPublisherKit.videoTransformers` property.

## Creating a custom audio transformer

Create a class that implements the <a href="/developer/sdks/android/reference/com/opentok/android/PublisherKit.CustomAudioTransformer.html"><code>PublisherKit.CustomAudioTransformer</code></a> 
interface. Implement the `PublisherKit.CustomAudioTransformer.onTransform​(AudioData audioData)` method.
The `PublisherKit.CustomAudioTransformer.onTransform​(AudioData audioData)` method is triggered for each audio frame.
In the implementation of the method, apply a transformation to the `frame` object passed into the method.
The following example applies a simple amplitude limiter on the audio:

```java
public class MyCustomAudioTransformer implements PublisherKit.CustomAudioTransformer {
    private short CROP_LIMIT = (short)(32767 * 0.05);
    @Override
    public void onTransform(AudioData audioData) {
        int samplesPerChannel = (int)audioData.getNumberOfSamples() * (int)audioData.getNumberOfChannels();
        ShortBuffer samples = audioData.getSampleBuffer().asShortBuffer();
        for (int s = 0; s < samplesPerChannel; ++s) {
            short sample = samples.get(s);
            if (sample > CROP_LIMIT)
                samples.put(s, CROP_LIMIT);
            else if (sample < -CROP_LIMIT)
                samples.put(s, (short)-CROP_LIMIT);
        }
    }
}
```

Then pass the object that implements the PublisherKit.CustomAudioTransformer interface into the `PublisherKit.setAudioTransformers()` method:

```java
  Publisher publisher = new Publisher.Builder(MainActivity.this).build();
  MyCustomAudioTransformer transformer = new MyCustomAudioTransformer();
  PublisherKit.AudioTransformer myCustomTransformer = publisher.new AudioTransformer("myTransformer", transformer);
  ArrayList<PublisherKit.VideoTransformer> audioTransformers = new ArrayList<>();
  audioTransformers.add(myCustomTransformer);
  publisher.setAudioTransformers(audioTransformers);
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

## Clearing audio transformers for a publisher

To clear audio transformers for a publisher, pass an empty ArrayList into
into the `PublisherKit.setAudioTransformers()` method.

```java
audioTransformers.clear();
mPublisher.setAudioTransformers(videoTransformers);
```
