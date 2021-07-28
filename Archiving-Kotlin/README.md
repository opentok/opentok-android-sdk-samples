# Archiving

This application provides a completed version of the [OpenTok Archiving tutorial](https://tokbox.com/developer/tutorials/android/archiving/) for Android (differing only in some additional validation checks). Upon deploying this sample application, you should be able to start, stop, and view recordings of your sessions.
## Deploy a sample backend web service
Recording archives are stored on OpenTok cloud (not on the user device), so we'll need to set up a web service that communicates with OpenTok cloud to start and stop archiving.

In order to archive OpenTok sessions, you need to have a server set up (hardcoded session information will not work for archiving). To quickly deploy a pre-built server click at one of the Heroku buttons below. You'll be sent to Heroku's website and prompted for your OpenTok `API Key` and `API Secret` — you can obtain these values on your project page in your [TokBox account](https://tokbox.com/account/user/signup). If you don't have a Heroku account, you'll need to sign up (it's free).

| PHP server  | Node.js server|
| ------------- | ------------- |
| <a href="https://heroku.com/deploy?template=https://github.com/opentok/learning-opentok-php" target="_blank"> <img src="https://www.herokucdn.com/deploy/button.png" alt="Deploy"></a>  | <a href="https://heroku.com/deploy?template=https://github.com/opentok/learning-opentok-node" target="_blank"> <img src="https://www.herokucdn.com/deploy/button.png" alt="Deploy"></a>  |
| [Repository](https://github.com/opentok/learning-opentok-php) | [Repository](https://github.com/opentok/learning-opentok-node) |

This sample web service provides a RESTful interface to interact with archiving controls. 

> Note: You can also build your server from scratch using one of the [server SDKs](https://tokbox.com/developer/sdks/server/).

## Configure the app to use your web service
After deploying the server open the `ServerConfig` file in this project and configure the `CHAT_SERVER_URL` with your domain to fetch credentials from the server:

```java
public static final String CHAT_SERVER_URL = "https://YOURAPPNAME.herokuapp.com";
```

The endpoints of the web service the app calls to start archive recording, stop recording, and play back the recorded video are defined in `APIService` interface:

```
public interface APIService {
    @GET("session")
    Call<GetSessionResponse> getSession();

    @POST("archive/start")
    @Headers("Content-Type: application/json")
    Call<Void> startArchive(@Body StartArchiveRequest startArchiveRequest);

    @POST("archive/{archiveId}/stop")
    Call<Void> stopArchive(@Path("archiveId") String archiveId);
}
```

## Start archiving

When the user clicks the `start archive` button, the app calls `archive/start` endpoint via `startArchive` method:

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

When archive recording starts, the `onArchiveStarted` callback is triggered:

```java
@Override
public void onArchiveStarted(Session session, String archiveId, String archiveName) {
    currentArchiveId = archiveId;
    setStopArchiveEnabled(true);
    archivingIndicatorView.setVisibility(View.VISIBLE);
}
```

The `onArchiveStarted` method stores the archive identifier in a `currentArchiveId` property. The method also calls the `setStopArchiveEnabled(true)` method, which causes the `stop recording` menu item to be displayed. And it causes the `archivingIndicatorView` to be displayed (red dot on the video).

## Stop archiving

When the user clicks the `stop archive` button, the app calls `archive/stop` endpoint via `startArchive` method:

```java
private void stopArchive() {
    Log.i(TAG, "stopArchive");

    Call call = apiService.stopArchive(currentArchiveId);
    call.enqueue(new EmptyCallback());
    setStopArchiveEnabled(false);
}
```

When archive recording stops, the `onArchiveStopped` callback is triggered:

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

The `onArchiveStopped` method stores the archive identifier to a `playableArchiveId` property
and sets `currentArchiveId` to `null`. The method also calls the `setPlayArchiveEnabled(false)`
method, which disables the Play Archive menu item, and it calls `setStartArchiveEnabled(true)` to
enable the Start Archive menu item. And it causes the `archivingIndicatorView` to be hidden.

## Viewing recorded archives

When the user clicks the play archive button, the `playArchive` method opens a web page (in the device's web browser) that displays the archive recording:

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