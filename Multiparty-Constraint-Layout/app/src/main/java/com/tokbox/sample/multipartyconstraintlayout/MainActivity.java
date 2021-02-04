package com.tokbox.sample.multipartyconstraintlayout;

import android.Manifest;
import android.content.res.TypedArray;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    
    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Session session;
    private Publisher publisher;

    private ArrayList<Subscriber> subscribers = new ArrayList<>();
    private HashMap<Stream, Subscriber> subscriberStreams = new HashMap<>();

    private ConstraintLayout container;

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

            session.publish(publisher);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

            session = null;
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

            final Subscriber subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
            session.subscribe(subscriber);
            subscribers.add(subscriber);
            subscriberStreams.put(stream, subscriber);

            int subId = getResIdForSubscriberIndex(subscribers.size() - 1);
            subscriber.getView().setId(subId);
            container.addView(subscriber.getView());

            calculateLayout();
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

            Subscriber subscriber = subscriberStreams.get(stream);

            if (subscriber == null) {
                return;
            }

            subscribers.remove(subscriber);
            subscriberStreams.remove(stream);
            
            container.removeView(subscriber.getView());

            // Recalculate view Ids
            for (int i = 0; i < subscribers.size(); i++) {
                subscribers.get(i).getView().setId(getResIdForSubscriberIndex(i));
            }
            calculateLayout();
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

        container = findViewById(R.id.main_container);

        requestPermissions();
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
        finishWithMessage("onPermissionsDenied: " + requestCode + ":" + perms.size());
    }

    private int getResIdForSubscriberIndex(int index) {
        TypedArray arr = getResources().obtainTypedArray(R.array.subscriber_view_ids);
        int subId = arr.getResourceId(index, 0);
        arr.recycle();
        return subId;
    }

    private void startPublisherPreview() {
        publisher = new Publisher.Builder(this).build();
        publisher.setPublisherListener(publisherListener);
        publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        publisher.startPreview();
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };

        if (EasyPermissions.hasPermissions(this, perms)) {
            session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).sessionOptions(new Session.SessionOptions() {
                @Override
                public boolean useTextureViews() {
                    return true;
                }
            }).build();
            session.setSessionListener(sessionListener);
            session.connect(OpenTokConfig.TOKEN);

            startPublisherPreview();
            publisher.getView().setId(R.id.publisher_view_id);
            container.addView(publisher.getView());
            calculateLayout();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    private void calculateLayout() {
        ConstraintSetHelper set = new ConstraintSetHelper(R.id.main_container);

        int size = subscribers.size();
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
            set.layoutViewHeightPercent(R.id.publisher_view_id, .5f);
            set.layoutViewHeightPercent(getResIdForSubscriberIndex(0), .5f);
        } else if (size > 1 && size % 2 == 0){
            //  Publisher
            // Sub1 | Sub2
            // Sub3 | Sub4
            //    .....
            int rows = (size / 2) + 1;
            float heightPercent = 1f / rows;

            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewHeightPercent(R.id.publisher_view_id, heightPercent);

            for (int i = 0; i < size; i += 2) {
                if (i == 0) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i + 1));
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i));
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1));
                }

                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1));
                set.layoutViewHeightPercent(getResIdForSubscriberIndex(i), heightPercent);
                set.layoutViewHeightPercent(getResIdForSubscriberIndex(i + 1), heightPercent);
            }

            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
        } else if (size > 1) {
            // Pub  | Sub1
            // Sub2 | Sub3
            // Sub3 | Sub4
            //    .....
            int rows = ((size + 1) / 2);
            float heightPercent = 1f / rows;

            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
            set.layoutViewHeightPercent(R.id.publisher_view_id, heightPercent);
            set.layoutViewWithTopBound(getResIdForSubscriberIndex(0), R.id.main_container);
            set.layoutViewHeightPercent(getResIdForSubscriberIndex(0), heightPercent);
            set.layoutTwoViewsOccupyingAllRow(R.id.publisher_view_id, getResIdForSubscriberIndex(0));

            for (int i = 1; i < size; i += 2) {
                if (i == 1) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
                    set.layoutViewHeightPercent(R.id.publisher_view_id, heightPercent);
                    set.layoutViewAboveView(getResIdForSubscriberIndex(0), getResIdForSubscriberIndex(i + 1));
                    set.layoutViewHeightPercent(getResIdForSubscriberIndex(0), heightPercent);
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i));
                    set.layoutViewHeightPercent(getResIdForSubscriberIndex(i - 2), heightPercent);
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1));
                    set.layoutViewHeightPercent(getResIdForSubscriberIndex(i - 1), heightPercent);
                }
                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1));
            }

            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
        }

        set.applyToLayout(container, true);
    }

    private void disconnectSession() {
        if (session == null) {
            return;
        }

        if (subscribers.size() > 0) {
            for (Subscriber subscriber : subscribers) {
                if (subscriber != null) {
                    session.unsubscribe(subscriber);
                }
            }
        }

        if (publisher != null) {
            session.unpublish(publisher);
            container.removeView(publisher.getView());
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
