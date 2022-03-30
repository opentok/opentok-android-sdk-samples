package com.tokbox.sample.basicvoipcall;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

@RequiresApi(api = Build.VERSION_CODES.M)
public class VoIPConnection extends Connection {

    private static final String TAG = "VoIPConnection";
    private final Context mContext;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    public VoIPConnection(Context Context) {
        super();
        Log.i(TAG, "VoIPConnection() ctor");
        this.mContext = Context;
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        Log.i(TAG, "onCallAudioStateChanged(state) || state = " + state);
        super.onCallAudioStateChanged(state);
    }

    @Override
    public void onStateChanged(int state) {
        Log.i(TAG, "onStateChanged()");
        super.onStateChanged(state);
    }

    @Override
    public void onDisconnect() {
        Log.i(TAG, "onDisconnect()");
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.BUSY));
        disconnectSession();
        destroy();
    }

    @Override
    public void onHold() {
        Log.i(TAG, "onHold()");
        super.onHold();
    }

    @Override
    public void onAnswer() {
        Log.i(TAG, "onAnswer()");
        connectSession();
        super.onAnswer();

        /*
        Intent myIntent = new Intent(mContext, MainActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(myIntent);
         */
    }

    @Override
    public void onReject() {
        Log.i(TAG, "onReject()");
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        disconnectSession();
        destroy();
    }

    @Override
    public void onShowIncomingCallUi() {
        Log.i(TAG, "onShowIncomingCallUi()");
        super.onShowIncomingCallUi();
    }


    private final PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
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
            Log.e(TAG, "PublisherKit error: " + opentokError.getMessage());
        }
    };

    private final Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
            publish();
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            Log.d(TAG, "Session error: " + opentokError.getMessage());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

            if (mSubscriber != null) {
                return;
            }

            subscribeToStream(stream);
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());
        }
    };

    private void connectSession() {
        mSession = new Session.Builder(mContext, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
        mSession.setSessionListener(sessionListener);
        mSession.connect(OpenTokConfig.TOKEN);
    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber.Builder(mContext, stream).build();
        mSession.subscribe(mSubscriber);
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
            mSession.unsubscribe(mSubscriber);
            mSubscriber = null;
        }

        if (mPublisher != null) {
            mSession.unpublish(mPublisher);
            mPublisher = null;
        }
        mSession.disconnect();
    }

    private void publish() {
        mPublisher = new Publisher.Builder(mContext).videoTrack(false).build();
        mPublisher.setPublisherListener(publisherListener);
        mSession.publish(mPublisher);
    }
}

