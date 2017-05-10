Basic Video Chat
===================================

This application provides a completed version of the OpenTok [Basic Video Chat tutorial](https://tokbox.com/developer/tutorials/android/) for Android (differing only in some additional validation checks). Upon deploying this sample application, you should be able to have two way audio and video communication using OpenTok.

Note that you will need a valid TokBox account for this and all other TokBox samples and tutorials.

### Quick Deploy
====================================

##### 1. Git clone this repository
Clone this repository using:

```git clone git@github.com:opentok/opentok-android-sdk-samples.git```

in your terminal. Then, using [Android Studio](https://developer.android.com/studio/index.html), open the project in the Basic-Video-Chat folder.

##### 2. Configure the app 
In `OpenTokConfig.java`, configure the `API_KEY`, `SESSION_ID`, and `TOKEN` variables. You can obtain these values from your [TokBox account](https://tokbox.com/account/#/). Make sure that your `TOKEN` isn't expired.

Note that we are hard coding these values in for demonstration purposes only. Your production app should not do this.

##### 2A. (Optional) Deploy a back end web service
If you want to see how you should run this app without hard coding config variables, you can deploy a pre-built server that we've provided. Do this by going to the [learning-opentok-php](https://github.com/opentok/learning-opentok-php) repository, and clicking on the purple deploy to Heroku button.

You can look through the details of that tutorial at a later point. For now, you just need to know that the sample web service provides a RESTful interface to give you the same Session information as you hard coded (`API_KEY`, `SESSION_ID`, and `TOKEN`).

##### 2B. (Optional) Configure the app to use your web service
In `OpenTokConfig.java`, configure the `CHAT_SERVER_URL` string to your web service domain.

##### 3. Run the app
That's it!