package com.tokbox.sample.framemetadata;

import com.opentok.android.BaseVideoCapturer;

class Test extends BaseVideoCapturer {
    @Override
    public void init() {

    }

    @Override
    public int startCapture() {
        return 0;
    }

    @Override
    public int stopCapture() {
        return 0;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isCaptureStarted() {
        return false;
    }

    @Override
    public CaptureSettings getCaptureSettings() {
        return null;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }
}
