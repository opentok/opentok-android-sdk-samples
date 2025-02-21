package com.tokbox.sample.basicvideochatwithforegroundservices;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.tokbox.sample.basicvideochatwithforegroundservices.network.APIService;
import com.tokbox.sample.basicvideochatwithforegroundservices.network.GetSessionResponse;
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


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CHANNEL_ID = "MyForegroundService";
    private static final String CHANNEL_NAME = "Audio Foreground Service";

    private static final int REQUEST_MICROPHONE_PERMISSIONS = 125;
    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Retrofit retrofit;
    private APIService apiService;

    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;

    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.getStreamId());
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.getStreamId());
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            finishWithMessage("PublisherKit onError: " + opentokError.getMessage());
        }
    };

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session: " + session.getSessionId());

            publisher = new Publisher.Builder(MainActivity.this).build();
            publisher.setPublisherListener(publisherListener);
            publisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            publisherViewContainer.addView(publisher.getView());

            if (publisher.getView() instanceof GLSurfaceView) {
                ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
            }

            session.publish(publisher);
            startMicrophoneForegroundService();
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: " + session.getSessionId());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received " + stream.getStreamId() + " in session: " + session.getSessionId());

            if (subscriber == null) {
                subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
                subscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
                subscriber.setSubscriberListener(subscriberListener);
                session.subscribe(subscriber);
                subscriberViewContainer.addView(subscriber.getView());
            }
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: " + stream.getStreamId() + " in session: " + session.getSessionId());

            if (subscriber != null) {
                subscriber = null;
                subscriberViewContainer.removeAllViews();
            }
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }
    };

    SubscriberKit.SubscriberListener subscriberListener = new SubscriberKit.SubscriberListener() {
        @Override
        public void onConnected(SubscriberKit subscriberKit) {
            Log.d(TAG, "onConnected: Subscriber connected. Stream: " + subscriberKit.getStream().getStreamId());
        }

        @Override
        public void onDisconnected(SubscriberKit subscriberKit) {
            Log.d(TAG, "onDisconnected: Subscriber disconnected. Stream: " + subscriberKit.getStream().getStreamId());
        }

        @Override
        public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
            finishWithMessage("SubscriberKit onError: " + opentokError.getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (!isBatteryOptimizationIgnored()) {
                requestBatteryOptimizationIntent();
            }
        }
        createNotificationChannel();
        requestPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // lifecycle of microphone foreground service is when audio is being published
        // you should also stop the foreground service if you call
        // publisher.setPublishAudio(false);
        stopMicrophoneForegroundService();
    }

    @Override
    protected void onPause() {
        if (session != null) {
            session.onPause();
        }
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session != null) {
            session.onResume();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Checks if battery optimizations are ignored for the app.
     *
     * @return true if the app is ignoring battery optimizations, false otherwise.
     */
    boolean isBatteryOptimizationIgnored() {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
    }

    /**
     * Setting Battery to Ignore Optimizations will prevent on Android 15 or above to disable network
     * connectivity when app is on background
     * +info: https://developer.android.com/reference/android/provider/Settings#ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
     *
     * @return The Intent to request battery optimization exemption, or null if already ignored.
     */
    void requestBatteryOptimizationIntent() {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        String packageName = getApplicationContext().getPackageName();

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    void startMicrophoneForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    void stopMicrophoneForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSIONS_REQUEST_CODE) {
            setupSession();
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) == PackageManager.PERMISSION_GRANTED) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
    }

    private void setupSession() {
        if (ServerConfig.hasChatServerUrl()) {
            // Custom server URL exists - retrieve session config
            if(!ServerConfig.isValid()) {
                finishWithMessage("Invalid chat server url: " + ServerConfig.CHAT_SERVER_URL);
                return;
            }

            initRetrofit();
            getSession();
        } else {
            // Use hardcoded session config
            if(!OpenTokConfig.isValid()) {
                finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
                return;
            }

            initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN);
        }
    }

    private void requestPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.INTERNET,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                }, PERMISSIONS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.INTERNET,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                }, PERMISSIONS_REQUEST_CODE);
            }
    }

    /* Make a request for session data */
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

        /*
        The context used depends on the specific use case, but usually, it is desired for the session to
        live outside of the Activity e.g: live between activities. For a production applications,
        it's convenient to use Application context instead of Activity context.
         */
        session = new Session.Builder(this, apiKey, sessionId).build();
        session.setSessionListener(sessionListener);
        session.connect(token);
    }

    private void initRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.CHAT_SERVER_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(APIService.class);
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }
}
