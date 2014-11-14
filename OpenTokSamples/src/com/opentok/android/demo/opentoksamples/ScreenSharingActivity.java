package com.opentok.android.demo.opentoksamples;

import java.net.MalformedURLException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpenTokConfig;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.PublisherKit.PublisherKitVideoType;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.demo.config.*;
import com.opentok.android.demo.screensharing.ScreensharingCapturer;
import com.opentok.android.demo.services.ClearNotificationService;
import com.opentok.android.demo.services.ClearNotificationService.ClearBinder;

public class ScreenSharingActivity extends Activity implements
Session.SessionListener, Publisher.PublisherListener,
Subscriber.VideoListener, Subscriber.SubscriberListener {

private static final String LOGTAG = "demo-hello-world";
private Session mSession;
private Publisher mPublisher;
private Subscriber mSubscriber;
private ArrayList<Stream> mStreams;
protected Handler mHandler = new Handler();

private WebView mPubScreenWebView;
private RelativeLayout mSubscriberViewContainer;

// Spinning wheel for loading subscriber view
private ProgressBar mLoadingSub;

private boolean resumeHasRun = false;

private boolean mIsBound = false;
private NotificationCompat.Builder mNotifyBuilder;
NotificationManager mNotificationManager;
ServiceConnection mConnection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOGTAG, "ONCREATE");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.screensharing_layout);

		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		mPubScreenWebView = (WebView) findViewById(R.id.webview_screen);
		
		mPubScreenWebView.setWebViewClient(new WebViewClient());
		WebSettings webSettings = mPubScreenWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		mPubScreenWebView.loadUrl("http://www.google.com");
		mPubScreenWebView.setVisibility(View.GONE);
		
		mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
		mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mStreams = new ArrayList<Stream>();

		 try {
        		//set environment
 	    	OpenTokConfig.setAPIRootURL("https://anvil-dev.opentok.com", false);
 	    	
 	    } catch (MalformedURLException e) {
             e.printStackTrace();
         }
 
		
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

			if (mSubscriber != null) {
				mSubscriberViewContainer.removeView(mSubscriber.getView());
			}
		}

		mNotifyBuilder = new NotificationCompat.Builder(this)
				.setContentTitle(this.getTitle())
				.setContentText(getResources().getString(R.string.notification))
				.setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

		Intent notificationIntent = new Intent(this, HelloWorldActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		mNotifyBuilder.setContentIntent(intent);
		if (mConnection == null) {
			mConnection = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName className,
						IBinder binder) {
					((ClearBinder) binder).service.startService(new Intent(
							ScreenSharingActivity.this,
							ClearNotificationService.class));
					NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNotificationManager.notify(
							ClearNotificationService.NOTIFICATION_ID,
							mNotifyBuilder.build());
				}

				@Override
				public void onServiceDisconnected(ComponentName className) {
					mConnection = null;
				}

			};
		}

		if (!mIsBound) {
			bindService(new Intent(ScreenSharingActivity.this,
					ClearNotificationService.class), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}

	}

	@Override
	public void onResume() {
		super.onResume();

		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}

		if (!resumeHasRun) {
			resumeHasRun = true;
			return;
		} else {
			if (mSession != null) {
				mSession.onResume();
			}
		}
		mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);

		reloadInterface();
	}

	@Override
	public void onStop() {
		super.onStop();

		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}

		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
		if (isFinishing()) {
			mNotificationManager
					.cancel(ClearNotificationService.NOTIFICATION_ID);
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

	public void restartAudioMode() {
		AudioManager Audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		Audio.setMode(AudioManager.MODE_NORMAL);
		this.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
	}

	private void sessionConnect() {
		if (mSession == null) {
			mSession = new Session(ScreenSharingActivity.this,
					com.opentok.android.demo.config.OpenTokConfig.API_KEY, com.opentok.android.demo.config.OpenTokConfig.SESSION_ID);
			mSession.setSessionListener(this);
			mSession.connect(com.opentok.android.demo.config.OpenTokConfig.TOKEN);
		}
	}

	@Override
	public void onConnected(Session session) {
		Log.i(LOGTAG, "Connected to the session.");
		
	}

	@Override
	public void onDisconnected(Session session) {
		Log.i(LOGTAG, "Disconnected from the session.");
		if (mSubscriber != null) {
			mSubscriberViewContainer.removeView(mSubscriber.getView());
		}

		mPublisher = null;
		mSubscriber = null;
		mStreams.clear();
		mSession = null;
	}

	private void subscribeToStream(Stream stream) {
		Log.i(LOGTAG, "mARINAS subscribeToStream");
		mSubscriber = new Subscriber(ScreenSharingActivity.this, stream);
		mSubscriber.setVideoListener(this);
		mSubscriber.setSubscriberListener(this);
		mSession.subscribe(mSubscriber);

		if (mSubscriber.getSubscribeToVideo()) {
			// start loading spinning
			mLoadingSub.setVisibility(View.VISIBLE);
		}
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
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				getResources().getDisplayMetrics().widthPixels, getResources()
						.getDisplayMetrics().heightPixels);
		mSubscriberViewContainer.removeView(mSubscriber.getView());
		mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
		subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
				BaseVideoRenderer.STYLE_VIDEO_FILL);
	}

	@Override
	public void onError(Session session, OpentokError exception) {
		Log.i(LOGTAG, "Session exception: " + exception.getMessage());
	}

	@Override
	public void onStreamReceived(Session session, Stream stream) {
		if (!com.opentok.android.demo.config.OpenTokConfig.SUBSCRIBE_TO_SELF) {
			mStreams.add(stream);
			if (mSubscriber == null) {
				subscribeToStream(stream);
			}
		}
	}

	@Override
	public void onStreamDropped(Session session, Stream stream) {
		if (!com.opentok.android.demo.config.OpenTokConfig.SUBSCRIBE_TO_SELF) {
			if (mSubscriber != null) {
				unsubscribeFromStream(stream);
			}
		}
	}

	@Override
	public void onStreamCreated(PublisherKit publisher, Stream stream) {
		if (com.opentok.android.demo.config.OpenTokConfig.SUBSCRIBE_TO_SELF) {
			mStreams.add(stream);
			if (mSubscriber == null) {
				subscribeToStream(stream);
			}
		}
	}

	@Override
	public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
		if ((com.opentok.android.demo.config.OpenTokConfig.SUBSCRIBE_TO_SELF && mSubscriber != null)) {
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

	@Override
	public void onVideoDisabled(SubscriberKit subscriber, String reason) {
		Log.i(LOGTAG, "Video disabled:" + reason);
	}

	@Override
	public void onVideoEnabled(SubscriberKit subscriber, String reason) {
		Log.i(LOGTAG, "Video enabled:" + reason);
	}

	@Override
	public void onVideoDisableWarning(SubscriberKit subscriber) {
		Log.i(LOGTAG,
				"Video may be disabled soon due to network quality degradation. Add UI handling here.");
	}

	@Override
	public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
		Log.i(LOGTAG,
				"Video may no longer be disabled as stream quality improved. Add UI handling here.");
	}

	
	@Override
	public void onConnected(SubscriberKit subscriber) {
		Log.i(LOGTAG, "Subscriber is connected: ");
		//Start screensharing when the subscriber is connected
		if (mPublisher == null) {
			mPublisher = new Publisher(ScreenSharingActivity.this, "publisher");
			mPublisher.setPublisherListener(this);
			mPublisher.setPublisherVideoType(PublisherKitVideoType.PublisherKitVideoTypeScreen);
			ScreensharingCapturer screenCapturer = new ScreensharingCapturer(this);
			mPublisher.setCapturer(screenCapturer);
			screenCapturer.setScreenView(mPubScreenWebView);
			mSession.publish(mPublisher);
		}
	
	}

	@Override
	public void onDisconnected(SubscriberKit subscriber) {
		Log.i(LOGTAG, "Subscriber is disconnected: ");
		//Stop screensharing when subscriber is disconnected
		if (mPublisher != null) {
			mSession.unpublish(mPublisher);
			mPublisher = null;
		}
	}

	@Override
	public void onError(SubscriberKit subscriber, OpentokError exception) {
		Log.i(LOGTAG, "Subscriber exception: " + exception.getMessage());
	}
}