package com.tokbox.sample.screensharing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import java.util.List;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_MEDIA_PROJECTION = 100;

    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Session session;
    private Publisher publisher;

    private RelativeLayout publisherViewContainer;
    private WebView webViewContainer;
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
            screenSharingManager.startForeground();
            startScreenCapture(resultCode, data);
        }
    }

    private void requestScreenCapturePermission() {
        Log.d(TAG, "Requesting permission to capture screen");
        mediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // This initiates a prompt dialog for the user to confirm screen projection.
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private void startScreenCapture(int resultCode, Intent data) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        ScreenSharingCapturer screenSharingCapturer = new ScreenSharingCapturer(MainActivity.this, mediaProjection);

        publisher = new Publisher.Builder(MainActivity.this)
                .capturer(screenSharingCapturer)
                .build();

        publisher.setPublisherListener(publisherListener);
        publisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
        publisher.setAudioFallbackEnabled(false);

        publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        publisherViewContainer.addView(publisher.getView());

        session.publish(publisher);
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
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());
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

        publisherViewContainer = findViewById(R.id.publisherview);
        webViewContainer = findViewById(R.id.webview);

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
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.FOREGROUND_SERVICE};
        
        if (EasyPermissions.hasPermissions(this, perms)) {
            session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
            session.setSessionListener(sessionListener);
            session.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }
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
