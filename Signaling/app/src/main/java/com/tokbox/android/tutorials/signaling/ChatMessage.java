package com.tokbox.android.tutorials.signaling;

public class ChatMessage {

    private String mMessageText;
    private Boolean mRemote;

    public ChatMessage(String messageText) {
        mMessageText = messageText;
        mRemote = false;
    }

    public static ChatMessage fromData(String messageData) {
        return new ChatMessage(messageData);
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
