package com.tokbox.pictureinpicturesample;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.FrameLayout;

import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

public class MainActivity extends Activity implements Session.SessionListener {
    
    public static final String API_KEY = "";
    public static final String TOKEN = "";
    public static final String SESSION_ID = "";


    private static final String TAG = MainActivity.class.getSimpleName();
    OpentokSingleton opentok = OpentokSingleton.getInstance();

    private FrameLayout mSubscriberViewContainer;
    private FrameLayout mPublisherViewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Activity Instance: " + this.toString());
        setContentView(R.layout.activity_main);

        mSubscriberViewContainer = findViewById(R.id.subscriber_container);
        mPublisherViewContainer = findViewById(R.id.publisher_container);

        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1000);
    }

    public void pipActivity(View view) {
        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(9,16)) // Portrait Aspect Ratio
                .build();
        enterPictureInPictureMode(params);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        if (opentok.getSession() == null) {
            Session s = new Session.Builder(getApplicationContext(), API_KEY, SESSION_ID)
                    .build();
            opentok.setSession(s);
        }
        opentok.getSession().setSessionListener(this);

        if (!opentok.isSessionConnected()) {
            opentok.getSession().connect(TOKEN);
        }

        if (opentok.getPublisher() != null && !isInPictureInPictureMode()) {
            mPublisherViewContainer.addView(opentok.getPublisher().getView());
            ((GLSurfaceView)opentok.getPublisher().getRenderer().getView()).setZOrderOnTop(true);
        }

        if (opentok.getSubscriber() != null) {
            mSubscriberViewContainer.addView(opentok.getSubscriber().getView());
        }

        if (isInPictureInPictureMode()) {
            findViewById(R.id.button).setVisibility(View.GONE);
            mPublisherViewContainer.setVisibility(View.GONE);
            getActionBar().hide();
        } else {
            findViewById(R.id.button).setVisibility(View.VISIBLE);
            mPublisherViewContainer.setVisibility(View.VISIBLE);
            getActionBar().show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!isInPictureInPictureMode()) {
            if (opentok.getSession() != null) {
                opentok.getSession().onPause();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!isInPictureInPictureMode()) {
            if (opentok.getSession() != null) {
                opentok.getSession().onResume();
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");

        if (opentok.getSubscriber() != null) {
            mSubscriberViewContainer.removeView(opentok.getSubscriber().getView());
        }

        if (opentok.getPublisher() != null) {
            mPublisherViewContainer.removeView(opentok.getPublisher().getView());
        }
    }

    // Session Listener
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "Session connected");
        opentok.setSessionConnected(true);

        if (opentok.getPublisher() == null) {
            Publisher p = new Publisher.Builder(getApplicationContext()).build();
            opentok.getSession().publish(p);

            mPublisherViewContainer.addView(p.getView());

            opentok.setPublisher(p);
        }

    }

    @Override
    public void onDisconnected(Session session) {
        opentok.setSessionConnected(false);
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        if (opentok.getSubscriber() == null) {
            Subscriber s = new Subscriber.Builder(getApplicationContext(), stream).build();
            opentok.setSubscriber(s);
            opentok.getSession().subscribe(s);
            mSubscriberViewContainer.addView(s.getView());

            if (opentok.getPublisher().getView() instanceof  GLSurfaceView) {
                ((GLSurfaceView) opentok.getPublisher().getView()).setZOrderOnTop(true);
            }
        } else {
            Log.d(TAG, "This sample supports just one subscriber");
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
    }
}
