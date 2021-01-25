package com.tokbox.sample.signaling.message;

/*
 * Each instance of this class represents a signal.
 *
 * The class has two properties, mMessageText, which contains the String content of each signal
 * and a boolean property, remote, that says whether the signal is one that is from the user's
 * client, or from another remote  client.
 *
 * This is not an OpenTok specific class. Its purpose is to organize this sample application's code.
 */

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
