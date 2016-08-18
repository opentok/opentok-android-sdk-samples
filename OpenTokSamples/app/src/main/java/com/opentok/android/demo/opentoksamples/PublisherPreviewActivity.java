package com.opentok.android.demo.opentoksamples;

/**
 * Created by rpc on 28/04/16.
 */

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.demo.config.OpenTokConfig;

import java.util.ArrayList;

public class PublisherPreviewActivity extends Activity implements
        Session.SessionListener, Publisher.PublisherListener,
        Subscriber.VideoListener {

    private static final String LOGTAG = "demo-hello-world";
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private ArrayList<Stream> mStreams;
    private Handler mHandler = new Handler();

    private LinearLayout mPublisherViewContainer;
    private LinearLayout mSubscriberViewContainer;
    private Button mPreviewButton;
    private Button mPublishButton;
    private Button mConnectButton;

    private Boolean wasPublishing = false;
    private Boolean wasPreviewing = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOGTAG, "ONCREATE");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.publisher_preview_layout);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mPublisherViewContainer = (LinearLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (LinearLayout) findViewById(R.id.subscriberview);
        mPreviewButton = (Button) findViewById(R.id.startPreviewButton);
        mPublishButton = (Button) findViewById(R.id.publishButton);
        mConnectButton = (Button) findViewById(R.id.connectButton);

        mStreams = new ArrayList<Stream>();
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
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSession != null) {
            mSession.onResume();
        }

        reloadInterface();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (isFinishing() && mSession != null) {
            mSession.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        if (mSession != null) {
            mSession.disconnect();
        }

        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mSession != null) {
            mSession.disconnect();
        }

        super.onBackPressed();
    }

    public void onClickConnect(View v) {
        if (mSession == null) {
            mSession = new Session(PublisherPreviewActivity.this,
                    OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        } else {
            if (wasPublishing) {
                mSession.unpublish(mPublisher);
                wasPublishing = false;
            }
            mSession.disconnect();
        }
    }

    public void onClickPublish(View v) {
        if (mPublisher == null) {
            mPublisher = new Publisher(PublisherPreviewActivity.this, "publisher");
        }
        if (!wasPublishing) {
            mSession.publish(mPublisher);
            mPublishButton.setText("Unpublish");
            wasPublishing = true;

            if (!wasPreviewing) {
                attachPublisherView(mPublisher);
            }
        } else {
            mSession.unpublish(mPublisher);

            if (!wasPreviewing) {
                dettachPublisherView(mPublisher);
            }
            mPublishButton.setText("Publish");
            wasPublishing = false;
        }
    }

    public void onClickPreview(View v) {
        if (mPublisher == null && !wasPreviewing) {
            mPublisher = new Publisher(PublisherPreviewActivity.this, "publisher");
            mPublisher.startPreview();
            attachPublisherView(mPublisher);
            mPreviewButton.setText("Stop Preview");
            wasPreviewing = true;
        } else if (mPublisher != null && wasPreviewing){
            dettachPublisherView(mPublisher);
            mPublisher.destroy();
            mPreviewButton.setText("Start Preview");
            wasPreviewing = false;
            mPublisher = null;
        }
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

    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "Connected to the session.");
        mConnectButton.setText("Disconnect");
        mPreviewButton.setEnabled(false);
        mPublishButton.setEnabled(true);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOGTAG, "Disconnected from the session.");
        mPublishButton.setEnabled(false);
        mConnectButton.setText("Connect");

        if (!wasPreviewing && mPublisher != null) {
            dettachPublisherView(mPublisher);

            mPreviewButton.setText("Start Preview");
            mPublisher.destroy();
            mPublisher = null;
        }

        mPreviewButton.setEnabled(true);
        mPublishButton.setText("Publish");

        if (mSubscriber != null) {
            dettachSubscriberView(mSubscriber);
            mSubscriber = null;
        }
        mStreams.clear();
        mSession = null;
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(PublisherPreviewActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }

    private void unsubscribeFromStream(Stream stream) {
        mStreams.remove(stream);
        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber = null;
            if (!mStreams.isEmpty()) {
                subscribeToStream(mStreams.get(0));
            }
        }
    }

    private void attachSubscriberView(Subscriber subscriber) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dettachSubscriberView(subscriber);
        mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void dettachSubscriberView(Subscriber subscriber) {
        mSubscriberViewContainer.removeView(subscriber.getView());
    }

    private void dettachPublisherView(Publisher publisher) {
        mPublisherViewContainer.removeView(publisher.getView());
    }

    private void attachPublisherView(Publisher publisher) {
        dettachPublisherView(publisher);
        publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPublisherViewContainer.addView(publisher.getView(), layoutParams);
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        Log.i(LOGTAG, "Session exception: " + exception.getMessage());
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
        Log.d(LOGTAG, "Stream dropped: " + stream.getStreamId());
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
        Log.i(LOGTAG, "Publisher stream destroyed");
        if ((OpenTokConfig.SUBSCRIBE_TO_SELF && mSubscriber != null)) {
            unsubscribeFromStream(stream);
        }
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOGTAG, "Publisher exception: " + exception.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOGTAG, "First frame received");
        attachSubscriberView(mSubscriber);
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOGTAG,
                "Video disabled:" + reason);
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriber, String reason) {
        Log.i(LOGTAG, "Video enabled:" + reason);
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriber) {
        Log.i(LOGTAG, "Video may be disabled soon due to network quality degradation. Add UI handling here.");
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
        Log.i(LOGTAG, "Video may no longer be disabled as stream quality improved. Add UI handling here.");
    }
}
