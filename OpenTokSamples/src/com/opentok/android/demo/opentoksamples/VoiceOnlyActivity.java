package com.opentok.android.demo.opentoksamples;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
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
import com.opentok.android.demo.opentokhelloworld.R;
import com.opentok.android.demo.ui.MeterView;
import com.opentok.android.demo.ui.MeterView.OnClickListener;

public class VoiceOnlyActivity extends Activity implements SessionListener,
		ArchiveListener {

	private static final String LOGTAG = "demo-voice-only";
	private Session mSession;
	private Publisher mPublisher;
	private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
	HashMap<Stream, Subscriber> mSubscriberStream = new HashMap<Stream, Subscriber>();
	MyAdapter mSubscriberAdapter = new MyAdapter(this, R.layout.voice_row);
	Handler mHandler = new Handler();

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

			meterView.setOnClickListener(new OnClickListener() {
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

	}

	@Override
	public void onResume() {
		super.onResume();

		if (mSession != null) {
			mSession.onResume();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		if (isFinishing()) {
			if (mSession != null) {
				mSession.disconnect();
			}
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
		meterView.setOnClickListener(new OnClickListener() {
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

	Runnable mHideStatus = new Runnable() {
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
