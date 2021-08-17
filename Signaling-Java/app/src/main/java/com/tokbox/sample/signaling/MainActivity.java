package com.tokbox.sample.signaling;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.tokbox.sample.signaling.message.SignalMessage;
import com.tokbox.sample.signaling.message.SignalMessageAdapter;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String SIGNAL_TYPE = "text-signal";

    private Session session;
    private SignalMessageAdapter messageHistory;

    private EditText messageEditTextView;
    private ListView messageHistoryListView;

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.i(TAG, "Session Connected");
            messageEditTextView.setEnabled(true);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.i(TAG, "Session Disconnected");
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.i(TAG, "Stream Received");
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.i(TAG, "Stream Dropped");
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }
    };

    private Session.SignalListener signalListener = new Session.SignalListener() {
        @Override
        public void onSignalReceived(Session session, String type, String data, Connection connection) {

            boolean remote = !connection.equals(session.getConnection());
            if (type != null && type.equals(SIGNAL_TYPE)) {
                showMessage(data, remote);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!OpenTokConfig.isValid()) {
            finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
            return;
        }

        messageEditTextView = findViewById(R.id.message_edit_text);
        messageHistoryListView = findViewById(R.id.message_history_list_view);

        // Attach data source to message history
        messageHistory = new SignalMessageAdapter(this);
        messageHistoryListView.setAdapter(messageHistory);

        // Attach handlers to UI
        messageEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        messageEditTextView.setEnabled(false);

        session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
        session.setSessionListener(sessionListener);
        session.setSignalListener(signalListener);
        session.connect(OpenTokConfig.TOKEN);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (session != null) {
            session.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session != null) {
            session.onResume();
        }
    }

    private void sendMessage() {
        Log.d(TAG, "Send Message");

        SignalMessage signal = new SignalMessage(messageEditTextView.getText().toString());
        session.sendSignal(SIGNAL_TYPE, signal.getMessageText());

        messageEditTextView.setText("");
    }

    private void showMessage(String messageData, boolean remote) {
        Log.d(TAG, "Show Message");

        SignalMessage message = new SignalMessage(messageData, remote);
        messageHistory.add(message);
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }
}
