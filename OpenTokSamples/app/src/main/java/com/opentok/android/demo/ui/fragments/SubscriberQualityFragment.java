package com.opentok.android.demo.ui.fragments;

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

import com.opentok.android.demo.opentoksamples.R;
import com.opentok.android.demo.opentoksamples.UIActivity;
import com.opentok.android.demo.ui.fragments.SubscriberControlFragment.SubscriberCallbacks;

public class SubscriberQualityFragment extends Fragment {

    private static final String LOGTAG = "demo-UI-sub-quality-fragment";

    private static final int ANIMATION_DURATION = 500;

    private boolean mSubscriberWidgetVisible = false;
    private ImageButton congestionIndicator;
    private RelativeLayout mSubQualityContainer;
    private UIActivity openTokActivity;

    private CongestionLevel congestion = CongestionLevel.Low;

    public enum CongestionLevel {
        High(2), Mid(1), Low(0);

        private int congestionLevel;

        private CongestionLevel(int congestionLevel) {
            this.congestionLevel = congestionLevel;
        }

        public int getCongestionLevel() {
            return congestionLevel;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(LOGTAG, "On attach Subscriber control fragment");
        openTokActivity = (UIActivity) activity;
        if (!(activity instanceof SubscriberCallbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callback");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.layout_fragment_sub_quality,
                container, false);

        mSubQualityContainer = (RelativeLayout) openTokActivity
                .findViewById(R.id.fragment_sub_quality_container);

        congestionIndicator = (ImageButton) rootView
                .findViewById(R.id.congestionIndicator);

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
    public void onDetach() {
        super.onDetach();

        Log.i(LOGTAG, "On detach Subscriber control fragment");
    }

    public void showSubscriberWidget(boolean show) {
        if (show) {
            switch (congestion) {
                case High:
                    this.congestionIndicator.setImageResource(R.drawable.high_congestion);
                    break;
                case Mid:
                    this.congestionIndicator.setImageResource(R.drawable.mid_congestion);
                    break;
                case Low:
                    break;

                default:
                    break;
            }
        } else {
            Log.i(LOGTAG, "Hidding subscriber quality");
        }

        showSubscriberWidget(show, true);

    }

    private void showSubscriberWidget(boolean show, boolean animate) {
        if (mSubQualityContainer != null) {
            mSubQualityContainer.clearAnimation();
            mSubscriberWidgetVisible = show;
            float dest = show ? 1.0f : 0.0f;
            AlphaAnimation aa = new AlphaAnimation(1.0f - dest, dest);
            aa.setDuration(animate ? ANIMATION_DURATION : 1);
            aa.setFillAfter(true);
            mSubQualityContainer.startAnimation(aa);

            if (show) {
                mSubQualityContainer.setVisibility(View.VISIBLE);
            } else {
                mSubQualityContainer.setVisibility(View.GONE);
            }
        }
    }

    public CongestionLevel getCongestion() {
        return congestion;
    }

    public void setCongestion(CongestionLevel high) {
        this.congestion = high;
    }

    public boolean isSubscriberWidgetVisible() {
        return mSubscriberWidgetVisible;
    }

    public RelativeLayout getSubQualityContainer() {
        return mSubQualityContainer;
    }
}
