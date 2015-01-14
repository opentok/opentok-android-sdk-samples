package com.opentok.android.demo.opentoksamples;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Session.ArchiveListener;
import com.opentok.android.Session.SessionListener;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.demo.config.OpenTokConfig;
import com.opentok.android.demo.services.ClearNotificationService;
import com.opentok.android.demo.services.ClearNotificationService.ClearBinder;
import com.opentok.android.demo.ui.MeterView;

import java.util.ArrayList;
import java.util.HashMap;

public class VoiceOnlyActivity extends Activity implements SessionListener,
        ArchiveListener {

    private static final String LOGTAG = "demo-voice-only";
    private Session mSession;
    private Publisher mPublisher;
    private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
    private HashMap<Stream, Subscriber> mSubscriberStream = new HashMap<Stream, Subscriber>();
    private MyAdapter mSubscriberAdapter = new MyAdapter(this, R.layout.voice_row);
    private Handler mHandler = new Handler();

    private boolean mIsBound = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;

    public class MyAdapter extends BaseAdapter {
        private final Context mContext;
        int mResource;

        MyAdapter(Context context, int resource) {
            super();
            this.mContext = context;
            this.mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(mResource, parent, false);

            final Subscriber subscriber = mSubscribers.get(position);

            // Set name
            final TextView name = (TextView) rowView
                    .findViewById(R.id.subscribername);
            name.setText(subscriber.getStream().getName());

            final ImageView picture = (ImageView) rowView
                    .findViewById(R.id.subscriberpicture);

            // Initialize meter view
            final MeterView meterView = (MeterView) rowView
                    .findViewById(R.id.volume);
            meterView.setIcons(BitmapFactory.decodeResource(getResources(),
                    R.drawable.unmute_sub), BitmapFactory.decodeResource(
                    getResources(), R.drawable.mute_sub));
            subscriber
                    .setAudioLevelListener(new SubscriberKit.AudioLevelListener() {
                        @Override
                        public void onAudioLevelUpdated(
                                SubscriberKit subscriber, float audioLevel) {
                            meterView.setMeterValue(audioLevel);
                        }
                    });

            meterView.setOnClickListener(new MeterView.OnClickListener() {
                @Override
                public void onClick(MeterView view) {
                    subscriber.setSubscribeToAudio(!view.isMuted());
                    float alpha = view.isMuted() ? 0.70f : 1.0f;
                    name.setAlpha(alpha);
                    picture.setAlpha(alpha);
                }
            });

            return rowView;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mSubscribers.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mSubscribers.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOGTAG, "ONCREATE");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.voice_only_layout);

        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(mSubscriberAdapter);

        // Set meter view icons for publisher
        MeterView mv = (MeterView) findViewById(R.id.publishermeter);
        mv.setIcons(BitmapFactory.decodeResource(getResources(),
                R.drawable.unmute_pub), BitmapFactory.decodeResource(
                getResources(), R.drawable.mute_pub));

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        sessionConnect();
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

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setContentText(getResources().getString(R.string.notification))
                .setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

        Intent notificationIntent = new Intent(this, VoiceOnlyActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        if (mConnection == null) {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    ((ClearBinder) binder).service.startService(new Intent(VoiceOnlyActivity.this, ClearNotificationService.class));
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.notify(ClearNotificationService.NOTIFICATION_ID, mNotifyBuilder.build());
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mConnection = null;
                }

            };
        }

        if (!mIsBound) {
            bindService(new Intent(VoiceOnlyActivity.this,
                            ClearNotificationService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;
            startService(notificationIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        if (mSession != null) {
            mSession.onResume();
        }

        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        if (isFinishing()) {
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            if (mSession != null) {
                mSession.disconnect();
            }
        }
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mSession != null) {
            mSession.disconnect();
        }
        restartAudioMode();

        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mSession != null) {
            mSession.disconnect();
        }
        restartAudioMode();

        super.onBackPressed();
    }

    public void restartAudioMode() {
        AudioManager Audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Audio.setMode(AudioManager.MODE_NORMAL);
        this.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
    }

    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(this, OpenTokConfig.API_KEY,
                    OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.setArchiveListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        }
    }

    public void onEndCall(View v) {
        finish();
    }

    @Override
    public void onConnected(Session session) {
        mPublisher = new Publisher(this, "Publisher");
        // Publish audio only
        mPublisher.setPublishVideo(false);
        mSession.publish(mPublisher);

        // Initialize publisher meter view
        final MeterView meterView = (MeterView) findViewById(R.id.publishermeter);
        mPublisher.setAudioLevelListener(new PublisherKit.AudioLevelListener() {
            @Override
            public void onAudioLevelUpdated(PublisherKit publisher,
                                            float audioLevel) {
                meterView.setMeterValue(audioLevel);
            }
        });
        meterView.setOnClickListener(new MeterView.OnClickListener() {
            @Override
            public void onClick(MeterView view) {
                mPublisher.setPublishAudio(!view.isMuted());
            }
        });
    }

    @Override
    public void onDisconnected(Session session) {
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Subscriber subscriber = new Subscriber(this, stream);

        // Subscribe audio only
        subscriber.setSubscribeToVideo(false);

        mSession.subscribe(subscriber);
        mSubscribers.add(subscriber);
        mSubscriberStream.put(stream, subscriber);
        mSubscriberAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Subscriber subscriber = mSubscriberStream.get(stream);
        if (subscriber != null) {
            mSession.unsubscribe(subscriber);
            mSubscribers.remove(subscriber);
            mSubscriberStream.remove(stream);
            mSubscriberAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onError(Session session, OpentokError error) {
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
    }

    private Runnable mHideStatus = new Runnable() {
        @Override
        public void run() {
            findViewById(R.id.archivingbar).setVisibility(View.GONE);
        }
    };

    private void setArchiving(int text, int img) {
        findViewById(R.id.archivingbar).setVisibility(View.VISIBLE);
        TextView statusText = (TextView) findViewById(R.id.archivingstatus);
        ImageView archiving = (ImageView) findViewById(R.id.archivingimg);
        statusText.setText(text);
        archiving.setImageResource(img);
        mHandler.removeCallbacks(mHideStatus);
        mHandler.postDelayed(mHideStatus, 5000);
    }

    @Override
    public void onArchiveStarted(Session session, String id, String name) {
        setArchiving(R.string.archivingOn, R.drawable.archiving_on);
    }

    @Override
    public void onArchiveStopped(Session session, String id) {
        setArchiving(R.string.archivingOff, R.drawable.archiving_off);
    }

}
