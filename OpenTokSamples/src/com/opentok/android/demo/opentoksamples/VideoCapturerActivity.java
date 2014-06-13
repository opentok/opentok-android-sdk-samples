package com.opentok.android.demo.opentoksamples;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.demo.config.OpenTokConfig;
import com.opentok.android.demo.opentokhelloworld.R;
import com.opentok.android.demo.video.CustomVideoCapturer;

public class VideoCapturerActivity extends Activity implements
        Session.SessionListener, Publisher.PublisherListener,
        Subscriber.VideoListener {

    private static final String LOGTAG = "demo-customer-video-capturer";

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private ArrayList<Stream> mStreams = new ArrayList<Stream>();
    protected Handler mHandler = new Handler();

    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberViewContainer;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSub;

    private boolean resumeHasRun = false;

    private NotificationCompat.Builder mNotifyBuilder;
    NotificationManager mNotificationManager;
    private int notificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_layout);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);

        mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mStreams = new ArrayList<Stream>();

        sessionConnect();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Remove publisher & subscriber views because we want to reuse them
        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
        }
        reloadInterface();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();

            if (mSubscriber != null) {
                mSubscriberViewContainer.removeView(mSubscriber.getView());
            }
        }

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setContentText(getResources().getString(R.string.notification))
                .setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

        Intent notificationIntent = new Intent(this,
                VideoCapturerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        notificationId = (int) System.currentTimeMillis();
        mNotificationManager.notify(notificationId, mNotifyBuilder.build());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!resumeHasRun) {
            resumeHasRun = true;
            return;
        } else {
            if (mSession != null) {
                mSession.onResume();
            }
        }
        mNotificationManager.cancel(notificationId);

        reloadInterface();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (isFinishing()) {
            mNotificationManager.cancel(notificationId);
            if (mSession != null) {
                mSession.disconnect();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mSession != null) {
            mSession.disconnect();
        }
        super.onBackPressed();
    }

    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriber != null) {
                    attachSubscriberView(mSubscriber);
                }
            }
        }, 500);
    }

    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(this, OpenTokConfig.API_KEY,
                    OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        }
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(VideoCapturerActivity.this, stream);
        mSubscriber.setVideoListener(this);
        // start loading spinning
        mLoadingSub.setVisibility(View.VISIBLE);
        mSession.subscribe(mSubscriber);
    }

    private void unsubscribeFromStream(Stream stream) {
        mStreams.remove(stream);
        if (mSubscriber.getStream().getStreamId().equals(stream.getStreamId())) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber = null;
            if (!mStreams.isEmpty()) {
                subscribeToStream(mStreams.get(0));
            }
        }
    }

    private void attachSubscriberView(Subscriber subscriber) {
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                        .getDisplayMetrics().heightPixels);
        mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
    }

    private void attachPublisherView(Publisher publisher) {
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                320, 240);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        layoutParams.bottomMargin = dpToPx(8);
        layoutParams.rightMargin = dpToPx(8);

        // use the default SDK video renderer
        mPublisherViewContainer.addView(mPublisher.getView(), layoutParams);
    }

    @Override
    public void onConnected(Session session) {

        if (mPublisher == null) {
            mPublisher = new Publisher(VideoCapturerActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            // use an external customer video capturer
            mPublisher.setCapturer(new CustomVideoCapturer(
                    VideoCapturerActivity.this));
            attachPublisherView(mPublisher);
            mSession.publish(mPublisher);
        }
    }

    @Override
    public void onDisconnected(Session session) {

        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getRenderer()
                    .getView());
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getRenderer()
                    .getView());
        }

        mPublisher = null;
        mSubscriber = null;
        mStreams.clear();
        mSession = null;

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            if (mSubscriber != null) {
                unsubscribeFromStream(stream);
            }
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {

        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            mStreams.add(stream);
            if (mSubscriber == null) {
                subscribeToStream(stream);
            }
        }
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            if (mSubscriber != null) {
                unsubscribeFromStream(stream);
            }
        }
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOGTAG, "Publisher exception: " + exception.getMessage());
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        Log.i(LOGTAG, "Session exception: " + exception.getMessage());
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriber) {
        Log.i(LOGTAG,
                "Video quality changed. It is disabled for the subscriber. ");
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOGTAG, "First frame received");

        // stop loading spinning
        mLoadingSub.setVisibility(View.GONE);
        attachSubscriberView(mSubscriber);
    }

    /**
     * Converts dp to real pixels, according to the screen density.
     * 
     * @param dp
     *            A number of density-independent pixels.
     * @return The equivalent number of real pixels.
     */
    private int dpToPx(int dp) {
        double screenDensity = this.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }

}
