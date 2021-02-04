# Archiving

This application provides a completed version of the [OpenTok Archiving tutorial](https://tokbox.com/developer/tutorials/android/archiving/) for Android (differing only in some additional validation checks). Upon deploying this sample application, you should be able to start, stop, and view recordings of your sessions.
## Deploy a sample backend web service
Recording archives are stored on OpenTok cloud (not on the user device), so we'll need to set up a web service that communicates with OpenTok cloud to start and stop archiving.

In order to archive OpenTok sessions, you need to have a server set up. There are many ways to implement archiving with a server, but for this tutorial we'll be quick-launching a [simple PHP server](https://github.com/opentok/learning-opentok-php). To launch the server, simply click the Heroku button below, at which point you'll be sent to Heroku's website and prompted for your OpenTok API Key and API Secret — you can get these values on your project page in your [TokBox account](https://tokbox.com/account/user/signup). If you don't have a Heroku account, you'll need to sign up (it's free).

<a href="https://heroku.com/deploy?template=https://github.com/opentok/learning-opentok-php" target="_blank">
  <img src="https://www.herokucdn.com/deploy/button.png" alt="Deploy">
</a>

This sample web service provides a RESTful interface to interact with archiving controls.

## Configure the app to use your web service
Open the `ServerConfig.java` file and configure the `CHAT_SERVER_URL` string to your web service domain:

```java
public static final String CHAT_SERVER_URL = "";
```

## Start archiving

When the user clicks stat archieve `startArchive` method is called and request is fired to the server:

```java
private void startArchive() {
    Log.i(TAG, "startArchive");

    if (session != null) {
        StartArchiveRequest startArchiveRequest = new StartArchiveRequest();
        startArchiveRequest.sessionId = sessionId;

        setStartArchiveEnabled(false);
        Call call = apiService.startArchive(startArchiveRequest);
        call.enqueue(new EmptyCallback());
    }
}
```

SDK notifies application about recording start via `onArchiveStarted()` callback (defined in `Session.ArchiveListener` interface):

```java
@Override
public void onArchiveStarted(Session session, String archiveId, String archiveName) {
    currentArchiveId = archiveId;
    setStopArchiveEnabled(true);
    archivingIndicatorView.setVisibility(View.VISIBLE);
}
```


## Stop archiving

When the user clicks stop archieve `stopArchive` method is called and request is fired to the server:

```java
private void stopArchive() {
    Log.i(TAG, "stopArchive");

    Call call = apiService.stopArchive(currentArchiveId);
    call.enqueue(new EmptyCallback());
    setStopArchiveEnabled(false);
}
```

SDK notifies application about recording stop via `onArchiveStopped()` callback (defined in `Session.ArchiveListener` interface):

```java
@Override
public void onArchiveStopped(Session session, String archiveId) {
    playableArchiveId = archiveId;
    currentArchiveId = null;
    setPlayArchiveEnabled(true);
    setStartArchiveEnabled(true);
    archivingIndicatorView.setVisibility(View.INVISIBLE);
}
```

The method stores the archive ID (identifying the archive) to a `playableArchiveId` property.
## View archives

When the user clicks the play archive button, the `playArchive()` method opens a web page (in the device's web browser) that displays the archive recording:

```java
private void playArchive() {
    Log.i(TAG, "playArchive");

    String archiveUrl = ServerConfig.CHAT_SERVER_URL + "/archive/" + playableArchiveId + "/view";
    Uri archiveUri = Uri.parse(archiveUrl);
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, archiveUri);
    startActivity(browserIntent);
}
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)