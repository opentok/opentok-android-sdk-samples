package com.tokbox.sample.pictureinpicture;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import pub.devrel.easypermissions.EasyPermissions;

import java.util.List;

public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;

    private FrameLayout subscriberViewContainer;
    private FrameLayout publisherViewContainer;

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "Session connected");

            if (publisher == null) {
                publisher = new Publisher.Builder(getApplicationContext()).build();
                session.publish(publisher);

                publisherViewContainer.addView(publisher.getView());

                if (publisher.getView() instanceof GLSurfaceView) {
                    ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
                }
            }
        }

        @Override
        public void onDisconnected(Session session) {
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            if (subscriber == null) {
                subscriber = new Subscriber.Builder(getApplicationContext(), stream).build();
                session.subscribe(subscriber);
                subscriberViewContainer.addView(subscriber.getView());
            } else {
                Log.d(TAG, "This sample supports just one subscriber");
            }
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            subscriberViewContainer.removeAllViews();
            subscriber = null;
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        subscriberViewContainer = findViewById(R.id.subscriber_container);
        publisherViewContainer = findViewById(R.id.publisher_container);

        String[] perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        if (isInPictureInPictureMode) {
            findViewById(R.id.button).setVisibility(View.GONE);
            publisherViewContainer.setVisibility(View.GONE);
            publisher.getView().setVisibility(View.GONE);
            getActionBar().hide();
        } else {
            findViewById(R.id.button).setVisibility(View.VISIBLE);
            publisherViewContainer.setVisibility(View.VISIBLE);
            publisher.getView().setVisibility(View.VISIBLE);
            if (publisher.getView() instanceof GLSurfaceView) {
                ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
            }
            getActionBar().show();
        }
    }

    public void pipActivity(View view) {
        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(9, 16)) // Portrait Aspect Ratio
                .build();
        enterPictureInPictureMode(params);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (session == null) {
            session = new Session.Builder(getApplicationContext(), OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID)
                    .build();
        }

        session.setSessionListener(sessionListener);
        session.connect(OpenTokConfig.TOKEN);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!isInPictureInPictureMode()) {
            if (session != null) {
                session.onPause();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInPictureInPictureMode()) {
            if (session != null) {
                session.onResume();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (subscriber != null) {
            subscriberViewContainer.removeView(subscriber.getView());
        }

        if (publisher != null) {
            publisherViewContainer.removeView(publisher.getView());
        }
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ":" + perms.size());
    }
}
