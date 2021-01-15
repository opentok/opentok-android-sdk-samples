# OpenTok Android SDK Samples

[![Build Status](https://travis-ci.org/opentok/opentok-android-sdk-samples.svg?branch=master)](https://travis-ci.org/opentok/opentok-android-sdk-samples)

This repository is meant to provide some examples for you to better understand the features of the [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/). For details on the full OpenTok Android API, see the [reference documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).

Feel free to copy and modify the source code herein for your own projects. Please consider sharing your modifications with us, especially if they might benefit other developers using the OpenTok Android SDK. See the [License](LICENSE) for more information.
## What's Inside

### Basic Video Chat

This app provides a completed version of the [OpenTok Basic Video Chat tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/) for Android (with some minor validation checks added). It shows how to publish and subscribe to streams in a session. If you're just getting started with OpenTok, this is where you should start ([More](/Basic-Video-Chat)).

### Archiving (recording)

This app provides a completed version of the [OpenTok Archiving tutorial](https://tokbox.com/developer/tutorials/android/archiving/) for Android (with some minor validation checks added). It uses an OpenTok server to start, stop, and play back recordings of your session ([More](/Archiving)). 

### Simple Multiparty

This app shows how to implement a simple video call application with several clients ([More](/Simple-Multiparty)).

### Multiparty using ConstraintLayout

This app shows how to use Android SDK ConstraintLayout to position all the video views of several participants in a multiparty session ([More](/Multiparty-ConstraintLayout)).

### Screen Sharing

This app shows how to to publish a screen-sharing video, using the device screen as the source for the stream's video ([More](/Screen-Sharing)).

### Live Photo Capture

This app shows how to capture an image from a subscribed video stream ([More](/Live-Photo-Capture)).

### Custom Video Driver

This app shows how to use both a custom video capturer and redender. While most applications will work fine with the default capturer (and therefore won't require an understanding of how the custom video driver work), if you need to add custom effects, then this is where you should start ([More](/Custom-Video-Driver)).

### Custom Audio Driver

This app shows how to use the audio driver API to implement a custom audio capturer and player. Just like the custom video driver, most applications will be fine using the default. If you want to add custom audio manipulation, look here ([More](/Custom-Audio-Driver)).

### Picture in Picture

This app shows how to use the “Picture-in-Picture” (PiP) mode to be able to continue seeing the video of a OpenTok session while navigating between apps or browsing content on the main screen of your phone ([More](/PictureInPicture)).

### Texture-Views-Renderer

This app shows how to use custom video renderer based on [TextureView](https://developer.android.com/reference/android/view/TextureView) class ([More](/Texture-Views-Renderer)).

### Signaling

This app shows how to utilize the OpenTok signaling API to send text messages to other clients connected to the OpenTok session ([More](/Signaling)).

### FrameMetadata

This app shows how to send\retrieve additional metadata associated with each video frame ([More](/FrameMetadata)).

### Custom-Capturer-Camera2

This app shows how to use the custom capturer using `camera2` API ([More](/Custom-Capturer-Camera2)).

## Development and Contributing

Interested in contributing? We :heart: pull requests! See the 
[Contribution](CONTRIBUTING.md) guidelines.

## Getting Help

We love to hear from you so if you have questions, comments or find a bug in the project, let us know! You can either:

- Open an issue on this repository
- See <https://support.tokbox.com/> for support options
- Tweet at us! We're [@VonageDev](https://twitter.com/VonageDev) on Twitter
- Or [join the Vonage Developer Community Slack](https://developer.nexmo.com/community/slack)

## Further Reading

Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
