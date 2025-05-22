package com.tokbox.sample.basicvideochat_connectionservice;

public interface CallEventListener {
    void onIncomingCall(String callerName, String callStatus);
}
