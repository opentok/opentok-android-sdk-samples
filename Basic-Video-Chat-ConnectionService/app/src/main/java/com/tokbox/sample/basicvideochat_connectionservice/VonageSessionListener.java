package com.tokbox.sample.basicvideochat_connectionservice;

import android.view.View;

public interface VonageSessionListener {
    void onPublisherViewReady(View view);
    void onSubscriberViewReady(View view);
    void onStreamDropped();
    void onError(String message);
}
