package com.tokbox.android.tutorials.signaling.message;

/*
 * An instance of this class provides a model for the client's local signaling history in a given OpenTok session.
 *
 * This is not an OpenTok specific class. Signals are not stored by the OpenTok platform, and the decision to
 * do so is an application specific one.
 *
 * This sample application stores signals in an instance of this class to then display in the text chat's UI.
 *
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tokbox.android.tutorials.signaling.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SignalMessageAdapter extends ArrayAdapter<SignalMessage> {

    public static final int VIEW_TYPE_LOCAL = 0;
    public static final int VIEW_TYPE_REMOTE = 1;
    private static final Map<Integer, Integer> viewTypes;
    static {
        Map<Integer, Integer> aMap = new HashMap<Integer, Integer>();
        aMap.put(VIEW_TYPE_LOCAL, R.layout.message_single_local);
        aMap.put(VIEW_TYPE_REMOTE, R.layout.message_single_remote);
        viewTypes = Collections.unmodifiableMap(aMap);
    }

    public SignalMessageAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SignalMessage message = getItem(position);

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

        SignalMessage message = getItem(position);
        return message.isRemote() ? VIEW_TYPE_REMOTE : VIEW_TYPE_LOCAL;
    }

    @Override
    public int getViewTypeCount() {
        return viewTypes.size();
    }
}
