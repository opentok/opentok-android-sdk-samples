# Opentok Android SDK Samples

<img src="https://assets.tokbox.com/img/vonage/Vonage_VideoAPI_black.svg" height="48px" alt="Tokbox is now known as Vonage" />

_Check documentation to understand [Video API basic concepts](https://tokbox.com/developer/guides/basics/). See instructions below in order to [open Android project](#open-project) and [set up the credentials](#set-up-credentials)._

The Android projects in this directory demonstrate typical use cases and features available in the [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/):

- Basic-Video-Chat ([Java](./Basic-Video-Chat-Java), [Kotlin](./Basic-Video-Chat-Kotlin)) demonstrates how to publish and subscribe to streams in a session. Best place to start
- Simple-Multiparty ([Java](./Simple-Multiparty-Java)) demonstrates how to enable/disable audio/video streams and how to swap camera
- Multiparty-Constraint-Layout ([Java](./Multiparty-Constraint-Layout-Java), [Kotlin](./Multiparty-Constraint-Layout-Kotlin)) demonstrates how to use [ConstraintLayout](https://developer.android.com/training/constraint-layout) to position all the videos in a multiparty session
- Signaling ([Java](./Signaling-Java)) demonstrates how to send text messages
- Archiving ([Java](./Archiving-Java), [Kotlin](./Archiving-Kotlin)) demonstrates how to utilize OpenTok server to start, stop, and playback session recordings
- Frame-Metadata ([Java](./Frame-Metadata-Java)) demonstrates how to send and receive additional metadata associated with each video frame
- Basic-Video-Capturer ([Java](./Basic-Video-Capturer-Java)) demonstrates how to create a custom video capturer using [Camera](https://developer.android.com/reference/android/hardware/Camera) class
- Basic-Video-Capturer-Camera-2 ([Java](./Basic-Video-Capturer-Camera-2-Java)) demonstrates how to create a custom video capturer using [Camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary) class
- Basic-Video-Renderer ([Java](./Basic-Video-Renderer-Java)) demonstrates how to create a custom video renderer
- Basic-Audio-Driver ([Java](./Basic-Audio-Driver-Java), [Kotlin](./Basic-Audio-Driver-Kotlin)) demonstrates how to publish a random audio signal and save audio streams to the file.
- Advanced-Audio-Driver ([Java](./Advanced-Audio-Driver-Java), [Kotlin](./Advanced-Audio-Driver-Kotlin)) demonstrates how to create a more advanced custom audio driver
- Basic-Video-Driver ([Java](./Basic-Video-Driver-Java)) demonstrates how to create a custom video driver
- Live-Photo-Capture ([Java](./Live-Photo-Capture-Java), [Kotlin](./Live-Photo-Capture-Kotlin)) demonstrates how to capture an image from a subscribed video stream
- Picture-In-Picture ([Java](./Picture-In-Picture-Java)) demonstrates how to use the [Picture-in-Picture](https://developer.android.com/guide/topics/ui/picture-in-picture) mode
- Screen-Sharing ([Java](./Screen-Sharing-Java), [Kotlin](./Screen-Sharing-Kotlin)) demonstrates how to publish a screen-sharing video, using the WebView as the source
- Phone-Call-Detection ([Java](./Phone-Call-Detection-Java), [Kotlin](./Phone-Call-Detection-Kotlin)) demonstrates how to detect incoming and outgoing phone calls
- ARCore-Integration ([Java](./ARCore-Integration-Java)) demonstrates how to use Google [ARCore](https://developers.google.com/ar) with Opentok
- Basic-VoIP-Call ([Java](./Basic-VoIP-Call-Java)) demonstrates how to use Android Connection Service (https://developer.android.com/reference/android/telecom/ConnectionService) with the OpenTok Android SDK.
- Video-Transformers ([Java](./Media-Transformers-Java), [Kotlin](./Media-Transformers-Kotlin)) demonstrates how to use pre-built transformers in the Vonage Media Processor library or create your own custom video transformer to apply to published video.
- E2EE-Video-Chat [Kotlin](./E2EE-Video-Chat-Kotlin) demonstrates how to have a two-way End to End Encrypted (E2EE) audio and video communication using OpenTok.
- Basic-Video-Chat-With-ForegroundServices [Java](./Basic-Video-Chat-With-ForegroundServices-Java)demonstrates how to setup foreground services in order to have a seamless user experience. And Kotlin version [Kotlin](./Basic-Video-Chat-With-ForegroundServices-Kotlin)
- Basic-Video-Chat-ConnectionService [Java](./Basic-Video-Chat-ConnectionService-Java)

## Open project

1. Clone this repository `git@githubx.com:opentok/opentok-android-sdk-samples.git`
2. Start [Android Studio](https://developer.android.com/studio)
3. In the `Quick Start` panel, click `Open an existing Android Studio Project`
4. Navigate to the repository folder, select the desired project subfolder, and click `Choose`

## Set up credentials

You will need a valid [TokBox account](https://tokbox.com/account/user/signup) for most of the sample projects. OpenTok credentials (`API_KEY`, `SESSION_ID`, `TOKEN`) are stored inside `OpenTokConfig` class. For these sample applications, credentials can be retrieved from the [Dashboard](https://dashboard.tokbox.com/projects) and hardcoded in the application, however for a production environment server should provide these credentials (check [Basic-Video-Chat](/Basic-Video-Chat) project). 

> Note: To facilitate testing connect to the same session using [OpenTok Playground](https://tokbox.com/developer/tools/playground/) (web client).

## Development and Contributing

Feel free to copy and modify the source code herein for your projects. Please consider sharing your modifications with us, especially if they might benefit other developers using the OpenTok Android SDK. See the [License](LICENSE) for more information.

Interested in contributing? You :heart: pull requests! See the 
[Contribution](CONTRIBUTING.md) guidelines.

### Testing

Projects consistancy is qurded by separate test project located in the `.test` directory. To run tests use these commands: 

1. `cd .test`
2. `./gradlew clean test`

## Getting Help

You love to hear from you so if you have questions, comments or find a bug in the project, let us know! You can either:

- Open an issue on this repository
- See <https://support.tokbox.com/> for support options
- Tweet at us! We're [@VonageDev](https://twitter.com/VonageDev) on Twitter
- Or [join the Vonage Developer Community Slack](https://developer.nexmo.com/community/slack)


