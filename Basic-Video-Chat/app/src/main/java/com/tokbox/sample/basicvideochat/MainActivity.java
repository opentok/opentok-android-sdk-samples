package com.tokbox.sample.basicvideochat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.opentok.android.*;
import com.tokbox.sample.basicvideochat.network.APIService;
import com.tokbox.sample.basicvideochat.network.GetSessionResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
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
        SubscriberKit.SubscriberListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Retrofit retrofit;
    private APIService apiService;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize view objects from your layout
        mPublisherViewContainer = (FrameLayout) findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout) findViewById(R.id.subscriber_container);

        requestPermissions();
    }

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

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setRationale(getString(R.string.rationale_ask_again))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel))
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {

            if (OpenTokConfig.hasChatServerUrl()) {
                // Custom server URL exists - retrieve session config
                OpenTokConfig.verifyChatServerUrl();
                initRetrofit();
                getSession();
            } else {
                // Use hardcoded session config
                OpenTokConfig.verifyConfig();
                initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN);
            }
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

    private void initializeSession(String apiKey, String sessionId, String token) {
        Log.i(LOG_TAG, "apiKey: " + apiKey);
        Log.i(LOG_TAG, "sessionId: " + sessionId);
        Log.i(LOG_TAG, "token: " + token);

        mSession = new Session.Builder(this, apiKey, sessionId).build();
        mSession.setSessionListener(this);
        mSession.connect(token);
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

    /* Session Listener methods */

    @Override
    public void onConnected(Session session) {

        Log.d(LOG_TAG, "onConnected: Connected to session: " + session.getSessionId());

        // initialize Publisher and set this object to listen to Publisher events
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        // set publisher video style to fill view
        mPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView) {
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {

        Log.d(LOG_TAG, "onDisconnected: Disconnected from session: " + session.getSessionId());
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.d(LOG_TAG, "onStreamReceived: New Stream Received " + stream.getStreamId() + " in session: " + session.getSessionId());

        if (mSubscriber == null) {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            mSubscriber.setSubscriberListener(this);
            mSession.subscribe(mSubscriber);
            mSubscriberViewContainer.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        Log.d(LOG_TAG, "onStreamDropped: Stream Dropped: " + stream.getStreamId() + " in session: " + session.getSessionId());

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

        Log.d(LOG_TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.getStreamId());

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

        Log.d(LOG_TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.getStreamId());
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

        logOpenTokError(opentokError);
    }

    @Override
    public void onConnected(SubscriberKit subscriberKit) {

        Log.d(LOG_TAG, "onConnected: Subscriber connected. Stream: " + subscriberKit.getStream().getStreamId());
    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {

        Log.d(LOG_TAG, "onDisconnected: Subscriber disconnected. Stream: " + subscriberKit.getStream().getStreamId());
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {

        logOpenTokError(opentokError);
    }

    private void logOpenTokError(OpentokError opentokError) {

        Log.e(LOG_TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(LOG_TAG, "Error Code: " + opentokError.getErrorCode().name());
    }
}
