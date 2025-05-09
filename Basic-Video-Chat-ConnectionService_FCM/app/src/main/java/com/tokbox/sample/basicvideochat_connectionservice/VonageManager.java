package com.tokbox.sample.basicvideochat_connectionservice;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

public class VonageManager {
    private static final String TAG = VonageManager.class.getSimpleName();

    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;
    private Context context;
    private final VonageSessionListener callback;
    private static VonageManager instance;

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.getStreamId());
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.getStreamId());
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            callback.onError("PublisherKit onError: " + opentokError.getMessage());
        }
    };

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session: " + session.getSessionId());

            publisher = new Publisher.Builder(context).build();
            publisher.setPublisherListener(publisherListener);
            publisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

            callback.onPublisherViewReady(publisher.getView());

            if (publisher.getView() instanceof GLSurfaceView) {
                ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
            }

            session.publish(publisher);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: " + session.getSessionId());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received " + stream.getStreamId() + " in session: " + session.getSessionId());

            if (subscriber == null) {
                subscriber = new Subscriber.Builder(context, stream).build();
                subscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
                subscriber.setSubscriberListener(subscriberListener);
                session.subscribe(subscriber);
                callback.onSubscriberViewReady(subscriber.getView());
            }
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: " + stream.getStreamId() + " in session: " + session.getSessionId());

            if (subscriber != null) {
                subscriber = null;
                callback.onStreamDropped();
            }
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            callback.onError("Session error: " + opentokError.getMessage());
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
            callback.onError("SubscriberKit onError: " + opentokError.getMessage());
        }
    };

    private VonageManager(Context context, VonageSessionListener callback) {
        this.context = context;
        this.callback = callback;
    }

    public static synchronized VonageManager getInstance(Context context, VonageSessionListener callback) {
        if (instance == null) {
            instance = new VonageManager(context, callback);
        }
        return instance;
    }

    public static synchronized VonageManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("VonageManager is not initialized. Call getInstance(context, callback) first.");
        }
        return instance;
    }

    public void initializeSession(String apiKey, String sessionId, String token) {
        Log.i(TAG, "apiKey: " + apiKey);
        Log.i(TAG, "sessionId: " + sessionId);
        Log.i(TAG, "token: " + token);

        session = new Session.Builder(this.context.getApplicationContext(), apiKey, sessionId).build();
        session.setSessionListener(sessionListener);
        session.connect(token);
    }

    public void onResume() {
        if (session != null) session.onResume();
    }

    public void onPause() {
        if (session != null) session.onPause();
    }

    public void endSession() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
        publisher = null;
        subscriber = null;
    }
}
