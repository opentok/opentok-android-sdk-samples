package com.tokbox.sample.simplemultiparty;

import android.Manifest;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
                          implements EasyPermissions.PermissionCallbacks,
                                     Publisher.PublisherListener,
                                     Session.SessionListener {

    static class SubscriberContainer {
        public RelativeLayout container;
        public ToggleButton toggleAudio;
        public Subscriber subscriber;

        public SubscriberContainer(RelativeLayout container,
                               ToggleButton toggleAudio,
                               Subscriber subscriber) {
            this.container = container;
            this.toggleAudio = toggleAudio;
            this.subscriber = subscriber;
        }
    }
    private static final String TAG = "simple-multiparty " + MainActivity.class.getSimpleName();

    private final int MAX_NUM_SUBSCRIBERS = 4;

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;

    private List<SubscriberContainer> mSubscribers;

    private RelativeLayout mPublisherViewContainer;

    private boolean sessionConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenTokConfig.verifyConfig();

        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);

        final Button swapCamera = (Button) findViewById(R.id.swapCamera);
        swapCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mPublisher == null) {
                    return;
                }
                mPublisher.cycleCamera();
            }
        });

        final ToggleButton toggleAudio = (ToggleButton) findViewById(R.id.toggleAudio);
        toggleAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPublisher == null) {
                    return;
                }
                if (isChecked) {
                    mPublisher.setPublishAudio(true);
                } else {
                    mPublisher.setPublishAudio(false);
                }
            }
        });

        final ToggleButton toggleVideo = (ToggleButton) findViewById(R.id.toggleVideo);
        toggleVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPublisher == null) {
                    return;
                }
                if (isChecked) {
                    mPublisher.setPublishVideo(true);
                } else {
                    mPublisher.setPublishVideo(false);
                }
            }
        });

        mSubscribers = new ArrayList<>();
        for (int i = 0; i < MAX_NUM_SUBSCRIBERS; i++) {
            int containerId = getResources().getIdentifier("subscriberview" + (new Integer(i)).toString(),
                    "id", this.getPackageName());
            int toggleAudioId = getResources().getIdentifier("toggleAudioSubscriber" + (new Integer(i)).toString(),
                    "id", this.getPackageName());
            mSubscribers.add(new SubscriberContainer(
                    findViewById(containerId),
                    findViewById(toggleAudioId),
                    null
            ));
        }

        requestPermissions();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
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
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

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
        String[] perms = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            mSession = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
        sessionConnected = true;

        mPublisher = new Publisher.Builder(this).name("publisher").build();

        mPublisher.setPublisherListener(this);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        mPublisherViewContainer.addView(mPublisher.getView());

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());
        sessionConnected = false;
        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    private SubscriberContainer findFirstEmptyContainer(Subscriber subscriber) {
        for (SubscriberContainer c : mSubscribers) {
            if (c.subscriber == null) {
                return c;
            }
        }
        return null;
    }

    private SubscriberContainer findContainerForStream(Stream stream) {
        for (SubscriberContainer c : mSubscribers) {
            if (c.subscriber.getStream().getStreamId().equals(stream.getStreamId())) {
                return c;
            }
        }
        return null;
    }

    private void addSubscriber(Subscriber subscriber) {
        SubscriberContainer container = findFirstEmptyContainer(subscriber);
        if (container == null) {
            Toast.makeText(this, "New subscriber ignored. MAX_NUM_SUBSCRIBERS limit reached.", Toast.LENGTH_LONG).show();
            return;
        }

        container.subscriber = subscriber;
        container.container.addView(subscriber.getView());
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        container.toggleAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                subscriber.setSubscribeToAudio(true);
            } else {
                subscriber.setSubscribeToAudio(false);
            }
        });
        container.toggleAudio.setVisibility(View.VISIBLE);
    }

    private void removeSubscriberWithStream(Stream stream) {
        SubscriberContainer container = findContainerForStream(stream);
        if (container == null) {
            return;
        }

        container.container.removeView(container.subscriber.getView());
        container.toggleAudio.setOnCheckedChangeListener(null);
        container.toggleAudio.setVisibility(View.INVISIBLE);
        container.subscriber = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        final Subscriber subscriber = new Subscriber.Builder(this, stream).build();
        mSession.subscribe(subscriber);
        addSubscriber(subscriber);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        removeSubscriberWithStream(stream);
    }

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
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void disconnectSession() {
        if (mSession == null || !sessionConnected) {
            return;
        }
        sessionConnected = false;

        if (mSubscribers.size() > 0) {
            for (SubscriberContainer c : mSubscribers) {
                if (c.subscriber != null) {
                    mSession.unsubscribe(c.subscriber);
                    c.subscriber.destroy();
                }
            }
        }

        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }
}
