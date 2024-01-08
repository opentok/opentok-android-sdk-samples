# E2EE Video Chat

Upon deploying this sample application, you should be able to have two-way End to End Encrypted (E2EE) audio and video communication using OpenTok.

Main features:
* Connect to an E2EE OpenTok session
* Publish an audio-video stream to the session
* Subscribe to another client's audio-video stream

# Configure the app 
Open the `OpenTokConfig` file and configure the `API_KEY`, `SESSION_ID`, and `TOKEN` variables. You can obtain these values from your [TokBox account](https://tokbox.com/account/#/).
Set the `SECRET` to your own Encryption Secret. A valid secret is a string between 8 and 256 characters.

# Enabling E2EE
To create an E2EE connection you must first enable this functionality server side.
You enable end-to-end encryption when you create a session using the REST API. Set the e2ee property to true. See [session creation](https://tokbox.com/developer/guides/create-session/).

The following Node.js example creates an end-to-end encryption enabled session:

```javascript
const opentok = new OpenTok(API_KEY, API_SECRET);
const sessionId;
opentok.createSession({
    mediaMode: 'routed',
    e2ee: true,
    }, function(error, session) {
    if (error) {
        console.log('Error creating session:', error)
    } else {
        sessionId = session.sessionId;
        console.log('Session ID: ' + sessionId);
    }
});
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
