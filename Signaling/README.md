# Signaling

This app shows how to utilize the OpenTok signaling API to send text messages to other clients connected to the OpenTok session. You can send a signal message to a specific client, or you can send a message to every client connected to the session.

> Note: If you aren't familiar with how to set up a basic video chat application, you should do that first. Check out the [Basic-Video-Chat](../Basic-Video-Chat) project, and [accompanying tutorial](https://tokbox.com/developer/tutorials/android/basic-video-chat/).

> Note: To facilicate testing you can connect to the session using [OpenTok Playground](https://tokbox.com/developer/tools/playground/) (web client).

## Using signalling

### Send signal

Signal can be send while client is connected to the seccion (after `Session.SessionListener.onConnected(session)` has been called and before `Session.SessionListener.onDisconnected(session)` method is called), so you need to set `Session.SessionListener`:

```java
mSession = new Session.Builder(this, apiKey, sessionId).build();
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(token);
```

Send signal to the session:

```java
SignalMessage signal = new SignalMessage(mMessageEditTextView.getText().toString());
mSession.sendSignal(SIGNAL_TYPE, signal.getMessageText());
```

### Receive signal

To listen for incomming messages set the `Session.SignalListener`:

```java
mSession = new Session.Builder(this, apiKey, sessionId).build();
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(token);
```

Process received signal inside `onSignalReceived` method:

```java
@Override
public void onSignalReceived(Session session, String type, String data, Connection connection) {

    boolean remote = !connection.equals(mSession.getConnection());
    if (type != null && type.equals(SIGNAL_TYPE)) {
        showMessage(data, remote);
    }
}
```

## Next steps

* Review [other sample projects](../)
* Read Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)