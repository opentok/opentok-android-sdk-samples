package com.tokbox.android.tutorials.signaling;

public class ChatMessage {

    private String mMessageText;
    private Boolean mRemote;

    public ChatMessage(String messageText) {
        mMessageText = messageText;
        mRemote = false;
    }

    public ChatMessage(String messageText, boolean remote) {
        mMessageText = messageText;
        mRemote = remote;
    }

    public static ChatMessage fromData(String messageData) {
        return new ChatMessage(messageData);
    }

    public static ChatMessage fromData(String messageData, boolean remote) {
        return new ChatMessage(messageData, remote);
    }

    public String getMessageText() {
        return mMessageText;
    }

    public Boolean getRemote() {
        return mRemote;
    }

    public void setRemote(Boolean remote) {
        mRemote = remote;
    }

    @Override
    public String toString() {
        return mMessageText;
    }
}
