package com.tokbox.sample.screensharing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
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

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final int PERMISSIONS_REQUEST_CODE = 124;
    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;

    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;

    private ScreenSharingManager screenSharingManager;
    private ScreenSharingCapturer screenSharingCapturer;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            finishWithMessage("PublisherKit error: " + opentokError.getMessage());
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != AppCompatActivity.RESULT_OK || data == null) {
                Toast.makeText(
                                this,
                                "screen_capture_permission_not_granted",
                                Toast.LENGTH_LONG)
                        .show();
                return;
            }
            startScreenCapture(resultCode, data);
        }
    }

    private void requestScreenCapturePermission() {
        Log.d(TAG, "Requesting permission to capture screen");
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private void startScreenCapture(int resultCode, Intent data) {
        if (screenSharingManager != null) {
            // Ensure the service is bound
            screenSharingManager.startForeground(); // this calls startForeground() in the service
    
            // Wait until the service is started in the foreground before requesting MediaProjection
            new Handler().postDelayed(() -> {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                ScreenSharingCapturer screenSharingCapturer =
                        new ScreenSharingCapturer(MainActivity.this, mediaProjection);

                publisher = new Publisher.Builder(MainActivity.this)
                        .capturer(screenSharingCapturer)
                        .build();

                publisher.setPublisherListener(publisherListener);
                publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
                publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

                publisherViewContainer.addView(publisher.getView());
                session.publish(publisher);

            }, 100); // small delay to ensure startForeground() has taken effect
        }
    }
    

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

            if (Build.VERSION.SDK_INT >= 34) {
                requestScreenCapturePermission();
            } else {
                requestScreenCapturePermission();
            }
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

            MainActivity.this.session = null;
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

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
            Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

            if (subscriber != null) {
                subscriber = null;
                subscriberViewContainer.removeAllViews();
            }
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
        screenSharingManager = new ScreenSharingManager(this);

        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);

        requestPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (session == null) {
            return;
        }

        session.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            return;
        }

        session.onResume();
    }

    @Override
    protected void onDestroy() {
        disconnectSession();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ": " + perms);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION};
        
        if (EasyPermissions.hasPermissions(this, perms)) {
            initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }
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

    private void disconnectSession() {
        if (session == null) {
            return;
        }

        if (publisher != null) {
            publisherViewContainer.removeView(publisher.getView());
            session.unpublish(publisher);
            publisher = null;
        }
        session.disconnect();
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }
}
