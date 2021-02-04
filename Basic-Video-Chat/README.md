# Basic Video Chat

This application provides a completed version of the OpenTok [Basic Video Chat tutorial](https://tokbox.com/developer/tutorials/android/) for Android (differing only in some additional validation checks). Upon deploying this sample application, you should be able to have two-way audio and video communication using OpenTok.

Main features:
* Connect to an OpenTok session
* Publish an audio-video stream to the session
* Subscribe to another client's audio-video stream

# Configure the app 
Open the `OpenTokConfig.java` file and configure the `API_KEY`, `SESSION_ID`, and `TOKEN` variables. You can obtain these values from your [TokBox account](https://tokbox.com/account/#/).
### (Optional) Deploy a back end web service

If you want to see how you should run this app without hard coding credentials (`API_KEY`, `SESSION_ID`, and `TOKEN`), you can deploy a pre-built server that we've provided ([learning-opentok-php](https://github.com/opentok/learning-opentok-php)). To launch the server, simply click the Heroku button below, at which point you'll be sent to Heroku's website and prompted for your OpenTok API Key and API Secret — you can get these values on your project page in your [TokBox account](https://tokbox.com/account/user/signup). If you don't have a Heroku account, you'll need to sign up (it's free).

<a href="https://heroku.com/deploy?template=https://github.com/opentok/learning-opentok-php" target="_blank">
  <img src="https://www.herokucdn.com/deploy/button.png" alt="Deploy">
</a>

Open the `ServerConfig.java` file and configure the `CHAT_SERVER_URL` string to your web service domain.

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
