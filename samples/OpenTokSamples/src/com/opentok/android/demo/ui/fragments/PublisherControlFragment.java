package com.opentok.android.demo.ui.fragments;

import com.opentok.android.demo.opentokhelloworld.R;
import com.opentok.android.demo.opentoksamples.UIActivity;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class PublisherControlFragment extends Fragment implements
		View.OnClickListener {

	private static final String LOGTAG = "demo-UI-pub-control-fragment";
	private static final int ANIMATION_DURATION = 500;
	private static final int PUBLISHER_CONTROLS_DURATION = 7000;

	private ImageButton mPublisherMute;
	private ImageButton mSwapCamera;
	private Button mEndCall;

	private PublisherCallbacks mCallbacks = sOpenTokCallbacks;
	private UIActivity openTokActivity;
	protected boolean mPublisherWidgetVisible = false;

	protected RelativeLayout mPublisherContainer;

	public interface PublisherCallbacks {
		public void onMutePublisher();

		public void onSwapCamera();

		public void onEndCall();
	}

	private static PublisherCallbacks sOpenTokCallbacks = new PublisherCallbacks() {

		@Override
		public void onMutePublisher() {
			return;
		}

		@Override
		public void onSwapCamera() {
			return;
		}

		@Override
		public void onEndCall() {
			return;
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		Log.i(LOGTAG, "On attach Publisher control fragment");
		openTokActivity = (UIActivity) activity;
		if (!(activity instanceof PublisherCallbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callback");
		}

		mCallbacks = (PublisherCallbacks) activity;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.layout_fragment_pub_control,
				container, false);

		mPublisherContainer = (RelativeLayout) openTokActivity
				.findViewById(R.id.fragment_pub_container);

		mPublisherMute = (ImageButton) rootView
				.findViewById(R.id.mutePublisher);
		mPublisherMute.setOnClickListener(this);

		mSwapCamera = (ImageButton) rootView.findViewById(R.id.swapCamera);
		mSwapCamera.setOnClickListener(this);

		mEndCall = (Button) rootView.findViewById(R.id.endCall);
		mEndCall.setOnClickListener(this);

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.i(LOGTAG, "On detach Publisher control fragment");
		mCallbacks = sOpenTokCallbacks;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.mutePublisher:
			mutePublisher();
			break;

		case R.id.swapCamera:
			swapCamera();
			break;

		case R.id.endCall:
			endCall();
			break;
		}
	}

	public void mutePublisher() {
		mCallbacks.onMutePublisher();

		mPublisherMute.setImageResource(openTokActivity.getmPublisher()
				.getPublishAudio() ? R.drawable.unmute_pub
				: R.drawable.mute_pub);
	}

	public void swapCamera() {
		mCallbacks.onSwapCamera();
	}

	public void endCall() {
		mCallbacks.onEndCall();
	}

	public void initPublisherUI() {
		openTokActivity.getmHandler()
				.removeCallbacks(mPublisherWidgetTimerTask);
		openTokActivity.getmHandler().postDelayed(mPublisherWidgetTimerTask,
				PUBLISHER_CONTROLS_DURATION);
		mPublisherMute.setImageResource(openTokActivity.getmPublisher()
				.getPublishAudio() ? R.drawable.unmute_pub
				: R.drawable.mute_pub);

	}

	private Runnable mPublisherWidgetTimerTask = new Runnable() {
		@Override
		public void run() {
			showPublisherWidget(false);
			openTokActivity.setPubViewMargins();
		}
	};

	public void publisherClick() {
		if (!mPublisherWidgetVisible) {
			showPublisherWidget(true);
		} else {
			showPublisherWidget(false);
		}
		initPublisherUI();
	}

	public void showPublisherWidget(boolean show) {
		showPublisherWidget(show, true);
	}

	private void showPublisherWidget(boolean show, boolean animate) {
		mPublisherContainer.clearAnimation();
		mPublisherWidgetVisible = show;
		float dest = show ? 1.0f : 0.0f;
		AlphaAnimation aa = new AlphaAnimation(1.0f - dest, dest);
		aa.setDuration(animate ? ANIMATION_DURATION : 1);
		aa.setFillAfter(true);
		mPublisherContainer.startAnimation(aa);

		if (show) {
			mPublisherContainer.setVisibility(View.VISIBLE);
		} else {
			mPublisherContainer.setVisibility(View.GONE);
		}

	}

	public boolean isMPublisherWidgetVisible() {
		return mPublisherWidgetVisible;
	}

	public RelativeLayout getMPublisherContainer() {
		return mPublisherContainer;
	}

}
