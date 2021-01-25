This repository provides some examples for you to understand better the [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/). For details on the full OpenTok Android API, see the [reference documentation](https://tokbox.com/opentok/libraries/client/android/reference/index.html).

Feel free to copy and modify the source code herein for your projects. Please consider sharing your modifications with us, especially if they might benefit other developers using the OpenTok Android SDK. See the [License](LICENSE) for more information.

> Credentials (`API_KEY`, `SESSION_ID`, `TOKEN`) are stored inside `OpenTokConfig` files of each project. For the sample applications, credentials can be retrieved from [OpenTOk Dashboard](https://dashboard.tokbox.com/projects) and hardcoded in the application. For a production environment server should provide the credentials. To quickly set up the server, see [server sample application] (https://github.com/opentok/learning-opentok-php) and [Basic-Video-Chat project](/Basic-Video-Chat). 
## What's Inside

### Basic Video Chat

This app provides a completed version of the [OpenTok Basic Video Chat tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/) for Android (with some minor validation checks added). It shows how to publish and subscribe to streams in a session. If you're just getting started with OpenTok, this is where you should start ([More](/Basic-Video-Chat)).

### Archiving (recording)

This app provides a completed version of the [OpenTok Archiving tutorial](https://tokbox.com/developer/tutorials/android/archiving/) for Android (with some minor validation checks added). It uses an OpenTok server to start, stop, and playback recordings of your session ([More](/Archiving)). 

### Simple Multiparty

This app shows how to implement a simple video call application with several clients ([More](/Simple-Multiparty)).

### Multiparty Constraint Layout

This app shows how to use Android SDK ConstraintLayout to position all the video views of several participants in a multiparty session ([More](/Multiparty-Constraint-Layout)).

### Screen Sharing

This app shows how to publish a screen-sharing video, using the device screen as the source for the stream's video ([More](/Screen-Sharing)).

### Live Photo Capture

This app shows how to capture an image from a subscribed video stream ([More](/Live-Photo-Capture)).

### Basic Video Renderer

This app shows how to use a custom video redender ([More](/Basic-Video-Renderer)).

### Custom Video Driver

This app shows how to use both a custom video capturer and a custom video redender ([More](/Custom-Video-Driver)).

### Basic Video Capturer

This app shows how to use a custom video capturer using [Camera](https://developer.android.com/reference/android/hardware/Camera) class deprecated in API level 21 ([More](/Basic-Video-Capturer)).

### Basic Video Capturer Camera 2

This app shows how to use a custom video capturer using [Camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary) class. ([More](/Basic-Video-Capturer-Camera-2)).

### Custom Audio Driver

This app shows how to use the audio driver API to implement a custom audio capturer and player. Just like the custom video driver, most applications will be fine using the default. If you want to add custom audio manipulation, look here ([More](/Custom-Audio-Driver)).

### Picture in Picture

This app shows how to use the “Picture-in-Picture” (PiP) mode to be able to continue seeing the video of a OpenTok session while navigating between apps or browsing content on the main screen of your phone ([More](/Picture-In-Picture)).

### Signaling

This app shows how to utilize the OpenTok signaling API to send text messages to other clients connected to the OpenTok session ([More](/Signaling)).

### Frame-Metadata

This app shows how to send\retrieve additional metadata associated with each video frame ([More](/Frame-Metadata)).

### ARCore Integration

This app shows how to use Google ARCore with Opentok ([More](/ARCore-Integration)).

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
