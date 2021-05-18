package com.tokbox.sample.basicvideorenderer;

import android.Manifest;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
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

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;

    private RelativeLayout publisherViewContainer;
    private LinearLayout subscriberViewContainer;

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

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

            publisher = new Publisher.Builder(MainActivity.this)
                    .renderer(new InvertedColorsVideoRenderer(MainActivity.this))
                    .build();

            publisher.setPublisherListener(publisherListener);
            publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            
            publisherViewContainer.addView(publisher.getView());

            if (publisher.getView() instanceof GLSurfaceView) {
                ((GLSurfaceView) (publisher.getView())).setZOrderOnTop(true);
            }

            session.publish(publisher);
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

            if (subscriber != null) {
                return;
            }

            subscribeToStream(stream);
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

            if (subscriber == null) {
                return;
            }

            if (subscriber.getStream().equals(stream)) {
                subscriberViewContainer.removeView(subscriber.getView());
                subscriber = null;
            }
        }
    };

    private Subscriber.VideoListener videoListener = new Subscriber.VideoListener() {
        @Override
        public void onVideoDataReceived(SubscriberKit subscriberKit) {
            subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            subscriberViewContainer.addView(subscriber.getView());
        }

        @Override
        public void onVideoDisabled(SubscriberKit subscriberKit, String s) { }

        @Override
        public void onVideoEnabled(SubscriberKit subscriberKit, String s) { }

        @Override
        public void onVideoDisableWarning(SubscriberKit subscriberKit) { }

        @Override
        public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) { }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        publisherViewContainer = findViewById(R.id.publisherview);
        subscriberViewContainer = findViewById(R.id.subscriberview);

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
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        
        if (EasyPermissions.hasPermissions(this, perms)) {
            session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
            session.setSessionListener(sessionListener);
            session.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    private void subscribeToStream(Stream stream) {
        subscriber = new Subscriber.Builder(this, stream)
                .renderer(new InvertedColorsVideoRenderer(this))
                .build();

        subscriber.setVideoListener(videoListener);
        session.subscribe(subscriber);
    }

    private void disconnectSession() {
        if (session == null) {
            return;
        }

        if (subscriber != null) {
            subscriberViewContainer.removeView(subscriber.getView());
            session.unsubscribe(subscriber);
            subscriber = null;
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
