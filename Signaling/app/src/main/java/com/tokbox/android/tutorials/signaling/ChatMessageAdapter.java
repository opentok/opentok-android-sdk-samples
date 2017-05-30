package com.tokbox.android.tutorials.signaling;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {

    public static final int VIEW_TYPE_LOCAL = 0;
    public static final int VIEW_TYPE_REMOTE = 1;
    private static final Map<Integer, Integer> viewTypes;
    static {
        Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
        aMap.put(VIEW_TYPE_LOCAL, R.layout.message_single_local);
        aMap.put(VIEW_TYPE_REMOTE, R.layout.message_single_remote);
        viewTypes = Collections.unmodifiableMap(aMap);
    }

    public ChatMessageAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ChatMessage message = getItem(position);

        if (convertView == null) {
            int type = getItemViewType(position);
            convertView = LayoutInflater.from(getContext()).inflate(viewTypes.get(type), null);
        }

        TextView messageTextView = (TextView)convertView.findViewById(R.id.message_text);
        if (messageTextView != null) {
            messageTextView.setText(message.getMessageText());
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        return message.getRemote() ? VIEW_TYPE_REMOTE : VIEW_TYPE_LOCAL;
    }

    @Override
    public int getViewTypeCount() {
        return viewTypes.size();
    }
}
