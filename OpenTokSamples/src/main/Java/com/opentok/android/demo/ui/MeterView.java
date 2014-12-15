package com.opentok.android.demo.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MeterView extends View {

	Context mContext;
	float mValue = 0;
	Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	Paint mPaintGradient = new Paint(Paint.ANTI_ALIAS_FLAG);
	Rect mBounds = new Rect();
	Bitmap mIconOn;
	Bitmap mIconOff;
	boolean mMute = false;
	OnClickListener mListener;

	public interface OnClickListener {
		public void onClick(MeterView view);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawCircle(mBounds.centerX(), mBounds.centerY(),
				mBounds.width() * 0.5f, mPaint);

		if (!mMute) {
			if (mIconOn != null) {
				canvas.drawBitmap(mIconOn,
						mBounds.centerX() - mIconOn.getWidth() * 0.5f,
						mBounds.centerY() - mIconOn.getHeight() * 0.5f, mPaint);
			}
			canvas.drawCircle(mBounds.centerX(), mBounds.centerY(),
					mBounds.width() * 0.5f * mValue, mPaintGradient);

		} else {
			if (mIconOff != null) {
				canvas.drawBitmap(mIconOff,
						mBounds.centerX() - mIconOff.getWidth() * 0.5f,
						mBounds.centerY() - mIconOff.getHeight() * 0.5f, mPaint);
			}
		}

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mBounds.left = (int) (0 + w * 0.10);
		mBounds.top = (int) (0 + h * 0.10);
		mBounds.right = (int) (w * 0.90);
		mBounds.bottom = (int) (h * 0.90);
		// Update gradient
		mPaintGradient.setShader(new RadialGradient(w / 2, h / 2, h / 2,
				0xff98CE00, 0x8098CE00, TileMode.CLAMP));

	}

	private void init() {
		mPaint.setStyle(Style.FILL);
		mPaint.setColor(0xff1f1f1f);
		mPaintGradient.setStyle(Style.FILL);
		mPaintGradient.setColor(0xff98CE00);
	}

	public MeterView(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public MeterView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public void setIcons(Bitmap iconOn, Bitmap iconOff) {
		mIconOn = iconOn;
		mIconOff = iconOff;
	}

	public void setMeterValue(float value) {
		// Convert linear value to logarithmic
		double db = 20 * Math.log10(value);
		float floor = -40;
		float level = 0;
		if (db > floor) {
			level = (float) db - floor;
			level /= -floor;
		}
		mValue = level;
		// force redraw
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = MotionEventCompat.getActionMasked(event);

		switch (action) {
		case (MotionEvent.ACTION_DOWN):
			mMute = !mMute;
			if (mListener != null) {
				mListener.onClick(this);
			}
			return true;
		default:
			return super.onTouchEvent(event);
		}
	}

	public boolean isMuted() {
		return mMute;
	}

	public void setOnClickListener(OnClickListener listener) {
		mListener = listener;
	}
}
