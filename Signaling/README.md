# Signaling

This app shows how to utilize the OpenTok signaling API to send text messages to other clients connected to the OpenTok session. You can send a signal message to a specific client, or you can send a message to every client connected to the session.

## Using signalling

### Send signal

Signal can be send while client is connected to the seccion (after `Session.SessionListener.onConnected(session)` has been called and before `Session.SessionListener.onDisconnected(session)` method is called), so you need to set `Session.SessionListener`:

```java
session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
session.setSessionListener(sessionListener);
session.setSignalListener(signalListener);
session.connect(OpenTokConfig.TOKEN);
```

Send signal to the session:

```java
SignalMessage signal = new SignalMessage(mMessageEditTextView.getText().toString());
session.sendSignal(SIGNAL_TYPE, signal.getMessageText());
```

### Receive signal

To listen for incomming messages set the `Session.SignalListener`:

```java
session = new Session.Builder(this, apiKey, sessionId).build();
session.setSessionListener(sessionListener);
session.setSignalListener(this);
session.connect(token);
```

Process received signal inside `onSignalReceived` method:

```java
private Session.SignalListener signalListener = new Session.SignalListener() {
    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {

        boolean remote = !connection.equals(session.getConnection());
        if (type != null && type.equals(SIGNAL_TYPE)) {
            showMessage(data, remote);
        }
    }
};
```

## Further Reading

* Review [other sample projects](../)
* Read Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)