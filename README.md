# OpenTok Android SDK Samples

[![Build Status](https://travis-ci.org/opentok/opentok-android-sdk-samples.svg?branch=master)](https://travis-ci.org/opentok/opentok-android-sdk-samples)

This repository is meant to provide some examples for you to better understand the features of the OpenTok Android SDK. The sample applications are meant to be used with the latest version of the [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/). Feel free to copy and modify the source code herein for your own projects. Please consider sharing your modifications with us, especially if they might benefit other developers using the OpenTok Android SDK. See the [License](LICENSE) for more information.

## What's Inside

### Basic Video Chat

This app provides a completed version of the [OpenTok Basic Video Chat tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/) for Android (with some minor validation checks added). It shows how to publish and subscribe to streams in a session. If you're just getting started with OpenTok, this is where you should start.

### Archiving (recording)

This app provides a completed version of the [OpenTok Archiving tutorial](https://tokbox.com/developer/tutorials/android/archiving/) for Android (with some minor validation checks added). It uses an OpenTok server to start, stop, and play back recordings of your session. 

### Simple Multiparty

This app shows how to implement a simple video call application with several clients.

### Multiparty using ConstraintLayout

This app shows how to use Android SDK ConstraintLayout to position all the video views of several participants in a multiparty session.

### Screen Sharing

This app shows how to publish a screen-sharing stream to a session.

### Live Photo Capture

This app shows how to capture an image from a subscribed video stream.

### Custom Video Driver

This app shows how to use both a custom video capturer and redender. While most applications will work fine with the default capturer (and therefore won't require an understanding of how the custom video driver work), if you need to add custom effects, then this is where you should start.

### Custom Audio Driver

This app shows how to use the audio driver API to implement a custom audio capturer and player. Just like the custom video driver, most applications will be fine using the default. If you want to add custom audio manipulation, look here.

### Picture in Picture

This app shows how to use the “Picture-in-Picture” (PiP) mode to be able to continue seeing the video of a OpenTok session while navigating between apps or browsing content on the main screen of your phone.

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

- Check out the Developer Documentation at <https://tokbox.com/developer/>