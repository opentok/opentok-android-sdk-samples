package com.tokbox.android.tutorials.signaling.message;

public class SignalMessage {

    private String mMessageText;

    private Boolean remote;

    public SignalMessage(String messageText) {

        this.mMessageText = messageText;
        this.remote = false;
    }

    public SignalMessage(String messageText, Boolean remote) {

        this.mMessageText = messageText;
        this.remote = remote;
    }

    public String getMessageText() {
        return this.mMessageText;
    }

    public void setMessageText(String mMessageText) {
        this.mMessageText = mMessageText;
    }

    public Boolean isRemote() {
        return this.remote;
    }

    public void setRemote(Boolean remote) {
        this.remote = remote;
    }

}
