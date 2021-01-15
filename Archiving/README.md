# Archiving
===================================

This application provides a completed version of the [OpenTok Archiving tutorial](https://tokbox.com/developer/tutorials/android/archiving/) for Android (differing only in some additional validation checks). Upon deploying this sample application, you should be able to start, stop, and play back recordings of your sessions.

> Note: If you aren't familiar with setting up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/). 

### Quick Start
====================================

##### 1. Deploy a sample back end web service
Because the actual archiving is not done on the user's device, but in the OpenTok cloud, you will need to set up a web service that communicates with it to start and stop archiving.

For the purposes of this tutorial, we'll be using a pre-built sample that we've provided. You can deploy this by going to the [learning-opentok-php](https://github.com/opentok/learning-opentok-php) repository, and clicking on the purple deploy to Heroku button.

You can look through the details of that tutorial at a later point. For now, you just need to know that the sample web service provides a RESTful interface to interact with Archiving controls.

##### 2. Clone this repository
Once you have the above service deployed, clone this repository using:

```git clone git@github.com:opentok/opentok-android-sdk-samples.git```

in your terminal. Using [Android Studio](https://developer.android.com/studio/index.html), open the project in the `Archiving` folder.

##### 3. Configure the app to use your web service
Open the `OpenTokConfig.java` file and configure the `CHAT_SERVER_URL` string to your web service domain.

##### 4. Run the app
That's it!

## Next steps

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)