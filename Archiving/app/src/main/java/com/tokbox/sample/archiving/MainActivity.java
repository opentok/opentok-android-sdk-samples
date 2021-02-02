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
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.tokbox.sample.archiving.network.APIService;
import com.tokbox.sample.archiving.network.EmptyCallback;
import com.tokbox.sample.archiving.network.GetSessionResponse;
import com.tokbox.sample.archiving.network.StartArchiveRequest;
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

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Retrofit retrofit;
    private APIService apiService;

    private String sessionId;
    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;
    private String currentArchiveId;
    private String playableArchiveId;

    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;
    private ImageView archivingIndicatorView;

    private Menu menu;

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.i(TAG, "Publisher Stream Created");
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.i(TAG, "Publisher Stream Destroyed");
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            logOpenTokError(opentokError);
        }
    };
    private Session.SessionListener sessionListener = new Session.SessionListener() {

        @Override
        public void onConnected(Session session) {
            Log.i(TAG, "Session Connected");

            // initialize Publisher and set this object to listen to Publisher events
            publisher = new Publisher.Builder(MainActivity.this).build();
            publisher.setPublisherListener(publisherListener);

            // set publisher video style to fill view
            publisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            publisherViewContainer.addView(publisher.getView(), 0);

            session.publish(publisher);

            setStartArchiveEnabled(true);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.i(TAG, "Session Disconnected");

            setStartArchiveEnabled(false);
            setStopArchiveEnabled(false);
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.i(TAG, "Stream Received");

            if (subscriber == null) {
                subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
                subscriber.setSubscriberListener(subscriberListener);
                subscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL);
                session.subscribe(subscriber);
            }
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.i(TAG, "Stream Dropped");

            if (subscriber != null) {
                subscriber = null;
                subscriberViewContainer.removeAllViews();
            }
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            logOpenTokError(opentokError);
        }
    };

    private SubscriberKit.SubscriberListener subscriberListener = new SubscriberKit.SubscriberListener() {
        @Override
        public void onConnected(SubscriberKit subscriberKit) {
            Log.i(TAG, "Subscriber Connected");

            subscriberViewContainer.addView(subscriber.getView());
        }

        @Override
        public void onDisconnected(SubscriberKit subscriberKit) {
            Log.i(TAG, "Subscriber Disconnected");
        }

        @Override
        public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
            logOpenTokError(opentokError);
        }
    };

    private Session.ArchiveListener archiveListener = new Session.ArchiveListener() {
        @Override
        public void onArchiveStarted(Session session, String archiveId, String archiveName) {
            currentArchiveId = archiveId;
            setStopArchiveEnabled(true);
            archivingIndicatorView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onArchiveStopped(Session session, String archiveId) {
            playableArchiveId = archiveId;
            currentArchiveId = null;
            setPlayArchiveEnabled(true);
            setStartArchiveEnabled(true);
            archivingIndicatorView.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fail fast if chat server URL is invalid
        OpenTokConfig.verifyChatServerUrl();

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);
        archivingIndicatorView = findViewById(R.id.archiving_indicator_view);

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
    protected void onPause() {
        super.onPause();

        if (session != null) {
            session.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session != null) {
            session.onResume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
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
        Log.i(TAG, "getSession");

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

    private void initializeSession(String apiKey, String sessionId, String token) {
        Log.i(TAG, "apiKey: " + apiKey);
        Log.i(TAG, "sessionId: " + sessionId);
        Log.i(TAG, "token: " + token);

        sessionId = sessionId;

        session = new Session.Builder(this, apiKey, sessionId).build();
        session.setSessionListener(sessionListener);
        session.setArchiveListener(archiveListener);
        session.connect(token);
    }

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

    private void stopArchive() {
        Log.i(TAG, "stopArchive");

        Call call = apiService.stopArchive(currentArchiveId);
        call.enqueue(new EmptyCallback());
        setStopArchiveEnabled(false);
    }

    private void playArchive() {

        Log.i(TAG, "playArchive");

        String archiveUrl = OpenTokConfig.CHAT_SERVER_URL + "/archive/" + playableArchiveId + "/view";
        Uri archiveUri = Uri.parse(archiveUrl);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, archiveUri);
        startActivity(browserIntent);
    }

    /* Activity lifecycle methods */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        this.menu = menu;
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

    private void setStartArchiveEnabled(boolean enabled) {
        menu.findItem(R.id.action_start_archive)
                .setEnabled(enabled)
                .setVisible(enabled);
    }

    private void setStopArchiveEnabled(boolean enabled) {
        menu.findItem(R.id.action_stop_archive)
                .setEnabled(enabled)
                .setVisible(enabled);
    }

    private void setPlayArchiveEnabled(boolean enabled) {
        menu.findItem(R.id.action_play_archive)
                .setEnabled(enabled)
                .setVisible(enabled);
    }

    private void logOpenTokError(OpentokError opentokError) {
        Log.e(TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(TAG, "Error Code: " + opentokError.getErrorCode().name());
    }
}
