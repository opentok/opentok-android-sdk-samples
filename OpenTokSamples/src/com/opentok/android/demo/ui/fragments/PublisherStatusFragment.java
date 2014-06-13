package com.opentok.android.demo.ui.fragments;

import com.opentok.android.demo.opentokhelloworld.R;
import com.opentok.android.demo.opentoksamples.UIActivity;
import com.opentok.android.demo.ui.fragments.PublisherControlFragment.PublisherCallbacks;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PublisherStatusFragment extends Fragment {

	private static final String LOGTAG = "demo-UI-pub-status-fragment";
	private static final int ANIMATION_DURATION = 500;
	private static final int STATUS_ANIMATION_DURATION = 7000;

	private ImageButton archiving;
	private TextView statusText;
	private UIActivity openTokActivity;
	protected boolean mPubStatusWidgetVisible = false;

	protected boolean archivingOn = false;

	protected RelativeLayout mPubStatusContainer;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		Log.i(LOGTAG, "On attach Publisher status fragment");
		openTokActivity = (UIActivity) activity;
		if (!(activity instanceof PublisherCallbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callback");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.layout_fragment_pub_status,
				container, false);

		mPubStatusContainer = (RelativeLayout) openTokActivity
				.findViewById(R.id.fragment_pub_status_container);
		archiving = (ImageButton) rootView.findViewById(R.id.archiving);

		statusText = (TextView) rootView.findViewById(R.id.statusLabel);

		if (openTokActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) container
					.getLayoutParams();

			DisplayMetrics metrics = new DisplayMetrics();
			openTokActivity.getWindowManager().getDefaultDisplay()
					.getMetrics(metrics);

			params.width = metrics.widthPixels - openTokActivity.dpToPx(48);
			container.setLayoutParams(params);
		}

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
		Log.i(LOGTAG, "On detach Publisher status fragment");
	}

	private Runnable mPubStatusWidgetTimerTask = new Runnable() {
		@Override
		public void run() {
			showPubStatusWidget(false);
			openTokActivity.setPubViewMargins();
		}
	};

	public void showPubStatusWidget(boolean show) {
		showPubStatusWidget(show, true);
	}

	private void showPubStatusWidget(boolean show, boolean animate) {

		mPubStatusContainer.clearAnimation();
		mPubStatusWidgetVisible = show;
		float dest = show ? 1.0f : 0.0f;
		AlphaAnimation aa = new AlphaAnimation(1.0f - dest, dest);
		aa.setDuration(animate ? ANIMATION_DURATION : 1);
		aa.setFillAfter(true);
		mPubStatusContainer.startAnimation(aa);

		if (show && archivingOn) {
			mPubStatusContainer.setVisibility(View.VISIBLE);
		} else {
			mPubStatusContainer.setVisibility(View.GONE);
		}
	}

	public void publisherClick() {
		if (!mPubStatusWidgetVisible) {
			showPubStatusWidget(true);
		} else {
			showPubStatusWidget(false);
		}

		initPubStatusUI();
	}

	public void initPubStatusUI() {
		openTokActivity.getmHandler()
				.removeCallbacks(mPubStatusWidgetTimerTask);
		openTokActivity.getmHandler().postDelayed(mPubStatusWidgetTimerTask,
				STATUS_ANIMATION_DURATION);
	}

	public void updateArchivingUI(boolean archivingOn) {

		archiving = (ImageButton) openTokActivity.findViewById(R.id.archiving);
		this.archivingOn = archivingOn;
		if (archivingOn) {
			statusText.setText(R.string.archivingOn);
			archiving.setImageResource(R.drawable.archiving_on);
			showPubStatusWidget(true);
			initPubStatusUI();
		}

		else {
			showPubStatusWidget(false);
		}
	}

	public boolean isMPubStatusWidgetVisible() {
		return mPubStatusWidgetVisible;
	}

	public RelativeLayout getMPubStatusContainer() {
		return mPubStatusContainer;
	}

}
