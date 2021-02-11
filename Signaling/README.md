# Signaling

This app shows how to utilize the OpenTok signaling API to send signals (text messages) to other clients connected to the OpenTok session. You can send a signal message to a specific client, or you can send a message to every client connected to the session.

## Using signalling

### Send a signal to all clients in a session

Signal can be send while client is connected to the seccion (after `Session.SessionListener.onConnected(session)` has been called and before `Session.SessionListener.onDisconnected(session)` method is called), so you need to set `Session.SessionListener`:

```java
session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
session.setSessionListener(sessionListener);
session.setSignalListener(signalListener);
session.connect(OpenTokConfig.TOKEN);
```

The `sendSignal` method (of the `Session` object_ is used to to send a signal to all clients in a session:

```java
SignalMessage signal = new SignalMessage(mMessageEditTextView.getText().toString());
session.sendSignal(SIGNAL_TYPE, signal.getMessageText());
```

The `SIGNAL_TYPE` parameter is a string value that clients can filter on when listening for signals (You'll see later in the `onSignalReceived` method). 

> You can set `SIGNAL_TYPE` to an empty string if you do not need to set a type or define multiple different types of signals.

> You can use a [REST API call(https://tokbox.com/developer/guides/signaling/rest/) to send a signal from your server, instead of from a client connected to the session.

### Receive signals in a session

To listen for incomming signals set the `Session.SignalListener` interface of the `Session` object:

```java
session = new Session.Builder(this, apiKey, sessionId).build();
session.setSessionListener(sessionListener);
session.setSignalListener(this);
session.connect(token);
```

The `onSignalReceived` method is called when the signal is received:

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

The `SIGNAL_TYPE` check is performed before processing the signal.

## Further Reading

* Review [other sample projects](../)
* Read Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)