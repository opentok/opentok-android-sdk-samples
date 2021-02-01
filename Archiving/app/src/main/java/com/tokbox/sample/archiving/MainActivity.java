package com.tokbox.sample.archiving;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.opentok.android.*;
import com.tokbox.sample.archiving.network.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        PublisherKit.PublisherListener,
        SubscriberKit.SubscriberListener,
        Session.ArchiveListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Retrofit retrofit;
    private APIService apiService;

    private String mSessionId;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private String mCurrentArchiveId;
    private String mPlayableArchiveId;

    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;
    private ImageView mArchivingIndicatorView;

    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fail fast if chat server URL is invalid
        OpenTokConfig.verifyChatServerUrl();

        mPublisherViewContainer = (FrameLayout) findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout) findViewById(R.id.subscriber_container);
        mArchivingIndicatorView = (ImageView) findViewById(R.id.archiving_indicator_view);

        requestPermissions();
    }

    private void initRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(OpenTokConfig.CHAT_SERVER_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(APIService.class);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(LOG_TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(LOG_TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            initRetrofit();
            getSession();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    // Make a request for session data
    private void getSession() {

        Log.i(LOG_TAG, "getSession");

        Call<GetSessionResponse> call = apiService.getSession();

        call.enqueue(new Callback<GetSessionResponse>() {
            @Override
            public void onResponse(Call<GetSessionResponse> call, Response<GetSessionResponse> response) {
                GetSessionResponse body = response.body();
                initializeSession(body.apiKey, body.sessionId, body.token);
            }

            @Override
            public void onFailure(Call<GetSessionResponse> call, Throwable t) {
                throw new RuntimeException(t.getMessage());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_start_archive:
                startArchive();
                return true;
            case R.id.action_stop_archive:
                stopArchive();
                return true;
            case R.id.action_play_archive:
                playArchive();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeSession(String apiKey, String sessionId, String token) {
        Log.i(LOG_TAG, "apiKey: " + apiKey);
        Log.i(LOG_TAG, "sessionId: " + sessionId);
        Log.i(LOG_TAG, "token: " + token);

        mSessionId = sessionId;

        mSession = new Session.Builder(this, apiKey, sessionId).build();
        mSession.setSessionListener(this);
        mSession.setArchiveListener(this);
        mSession.connect(token);
    }

    private void logOpenTokError(OpentokError opentokError) {
        Log.e(LOG_TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(LOG_TAG, "Error Code: " + opentokError.getErrorCode().name());
    }

    /* methods calling mWebServiceCoordinator to control Archiving */

    private void startArchive() {

        Log.i(LOG_TAG, "startArchive");

        if (mSession != null) {
            StartArchiveRequest startArchiveRequest = new StartArchiveRequest();
            startArchiveRequest.sessionId = mSessionId;

            setStartArchiveEnabled(false);
            Call call = apiService.startArchive(startArchiveRequest);
            call.enqueue(new EmptyCallback());
        }
    }

    private void stopArchive() {

        Log.i(LOG_TAG, "stopArchive");

        Call call = apiService.stopArchive(mCurrentArchiveId);
        call.enqueue(new EmptyCallback());
        setStopArchiveEnabled(false);
    }

    private void playArchive() {

        Log.i(LOG_TAG, "playArchive");

        String archiveUrl = OpenTokConfig.CHAT_SERVER_URL + "/archive/" +  mPlayableArchiveId + "/view";
        Uri archiveUri = Uri.parse(archiveUrl);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, archiveUri);
        startActivity(browserIntent);
    }

    /* Activity lifecycle methods */

    @Override
    protected void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSession != null) {
            mSession.onResume();
        }
    }

    // SessionListener methods

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");

        // initialize Publisher and set this object to listen to Publisher events
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        // set publisher video style to fill view
        mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView(), 0);

        mSession.publish(mPublisher);

        setStartArchiveEnabled(true);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");

        setStartArchiveEnabled(false);
        setStopArchiveEnabled(false);
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");

        if (mSubscriber == null) {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSubscriber.setSubscriberListener(this);
            mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL);
            mSession.subscribe(mSubscriber);
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber != null) {
            mSubscriber = null;
            mSubscriberViewContainer.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Publisher Listener methods */

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher Stream Created");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher Stream Destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Subscriber Listener methods */

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber Connected");

        mSubscriberViewContainer.addView(mSubscriber.getView());
    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber Disconnected");
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Archive Listener methods */

    @Override
    public void onArchiveStarted(Session session, String archiveId, String archiveName) {
        mCurrentArchiveId = archiveId;
        setStopArchiveEnabled(true);
        mArchivingIndicatorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onArchiveStopped(Session session, String archiveId) {
        mPlayableArchiveId = archiveId;
        mCurrentArchiveId = null;
        setPlayArchiveEnabled(true);
        setStartArchiveEnabled(true);
        mArchivingIndicatorView.setVisibility(View.INVISIBLE);
    }

    /* Options menu helpers */

    private void setStartArchiveEnabled(boolean enabled) {
        mMenu.findItem(R.id.action_start_archive)
                .setEnabled(enabled)
                .setVisible(enabled);
    }

    private void setStopArchiveEnabled(boolean enabled) {
        mMenu.findItem(R.id.action_stop_archive)
                .setEnabled(enabled)
                .setVisible(enabled);
    }

    private void setPlayArchiveEnabled(boolean enabled) {
        mMenu.findItem(R.id.action_play_archive)
                .setEnabled(enabled)
                .setVisible(enabled);
    }
}
