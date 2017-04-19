package com.opentok.android.samples.multiparty_constraintlayout;

import android.Manifest;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        Publisher.PublisherListener,
        Session.SessionListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;

    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();

    private ConstraintLayout mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContainer = (ConstraintLayout) findViewById(R.id.main_container);

        requestPermissions();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

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
        Log.d(TAG, "onDestroy");

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
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    private void startPublisherPreview() {
        mPublisher = new Publisher.Builder(this).name("publisher").build();

        mPublisher.setPublisherListener(this);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisher.startPreview();
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            startPublisherPreview();
            mPublisher.getView().setId(R.id.publisher_view_id);
            mContainer.addView(mPublisher.getView());
            calculateLayout();

            mSession = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).sessionOptions(new Session.SessionOptions() {
                @Override
                public boolean useTextureViews() {
                    return true;
                }
            }).build();
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    private int getResIdForSubscriberIndex(int index) {
        TypedArray arr = getResources().obtainTypedArray(R.array.subscriber_view_ids);
        int subId = arr.getResourceId(index, 0);
        arr.recycle();
        return subId;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        final Subscriber subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
        mSession.subscribe(subscriber);
        mSubscribers.add(subscriber);
        mSubscriberStreams.put(stream, subscriber);

        int subId = getResIdForSubscriberIndex(mSubscribers.size() - 1);
        subscriber.getView().setId(subId);
        mContainer.addView(subscriber.getView());

        calculateLayout();
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        Subscriber subscriber = mSubscriberStreams.get(stream);
        if (subscriber == null) {
            return;
        }

        mSubscribers.remove(subscriber);
        mSubscriberStreams.remove(stream);
        mContainer.removeView(subscriber.getView());

        // Recalculate view Ids
        for (int i = 0; i < mSubscribers.size(); i++) {
            mSubscribers.get(i).getView().setId(getResIdForSubscriberIndex(i));
        }
        calculateLayout();
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

    private void calculateLayout() {
        ConstraintSetHelper set = new ConstraintSetHelper(R.id.main_container);

        int size = mSubscribers.size();
        if (size == 0) {
            // Publisher full screen
            set.layoutViewFullScreen(R.id.publisher_view_id);
        } else if (size == 1) {
            // Publisher
            // Subscriber
            set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(0));
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(0), R.id.main_container);
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewAllContainerWide(getResIdForSubscriberIndex(0), R.id.main_container);

        } else if (size > 1 && size % 2 == 0){
            //  Publisher
            // Sub1 | Sub2
            // Sub3 | Sub4
            //    .....
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);

            for (int i = 0; i < size; i+=2) {
                if (i == 0) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i+1));
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i-2), getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i-1), getResIdForSubscriberIndex(i+1));
                }

                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i+1));
            }

            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
        } else if (size > 1) {
            // Pub  | Sub1
            // Sub2 | Sub3
            // Sub3 | Sub4
            //    .....

            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewWithTopBound(getResIdForSubscriberIndex(0), R.id.main_container);
            set.layoutTwoViewsOccupyingAllRow(R.id.publisher_view_id, getResIdForSubscriberIndex(0));

            for (int i = 1; i < size; i+=2) {
                if (i == 1) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(0), getResIdForSubscriberIndex(i+1));
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i-2), getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i-1), getResIdForSubscriberIndex(i+1));
                }
                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i+1));
            }

            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
        }

        set.applyToLayout(mContainer, true);
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscribers.size() > 0) {
            for (Subscriber subscriber : mSubscribers) {
                if (subscriber != null) {
                    mSession.unsubscribe(subscriber);
                    subscriber.destroy();
                }
            }
        }

        if (mPublisher != null) {
            mSession.unpublish(mPublisher);
            mContainer.removeView(mPublisher.getView());
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }
}
