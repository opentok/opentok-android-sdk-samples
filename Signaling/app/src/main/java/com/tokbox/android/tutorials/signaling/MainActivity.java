package com.tokbox.android.tutorials.signaling;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;

import com.tokbox.android.tutorials.signaling.message.SignalMessage;
import com.tokbox.android.tutorials.signaling.message.SignalMessageAdapter;


public class MainActivity extends AppCompatActivity
                            implements  WebServiceCoordinator.Listener,
                                        Session.SessionListener,
                                        Session.SignalListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String SIGNAL_TYPE = "text-signal";

    private WebServiceCoordinator mWebServiceCoordinator;

    private Session mSession;
    private SignalMessageAdapter mMessageHistory;

    private EditText mMessageEditTextView;
    private ListView mMessageHistoryListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate views
        mMessageEditTextView = (EditText)findViewById(R.id.message_edit_text);
        mMessageHistoryListView = (ListView)findViewById(R.id.message_history_list_view);

        // Attach data source to message history
        mMessageHistory = new SignalMessageAdapter(this);
        mMessageHistoryListView.setAdapter(mMessageHistory);

        // Attach handlers to UI
        mMessageEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        mMessageEditTextView.setEnabled(false);

        // initialize the session after validating configs

        // if there is no server URL assiged
        if (OpenTokConfig.CHAT_SERVER_URL == null) {
            // use hard coded session info
            if (OpenTokConfig.areHardCodedConfigsValid()) {
                initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN);
            } else {
                showConfigError("Configuration Error", OpenTokConfig.hardCodedConfigErrorMessage);
            }
        } else {
            // otherwise initialize WebServiceCoordinator and kick off request for session data
            // session initialization occurs once data is returned, in onSessionConnectionDataReady
            if (OpenTokConfig.isWebServerConfigUrlValid()) {
                mWebServiceCoordinator = new WebServiceCoordinator(this, this);
                mWebServiceCoordinator.fetchSessionConnectionData(OpenTokConfig.SESSION_INFO_ENDPOINT);
            } else {
                showConfigError("Configuration Error", OpenTokConfig.webServerConfigErrorMessage);
            }
        }
    }

    /* Activity lifecycle methods */

    @Override
    protected void onPause() {

        Log.d(LOG_TAG, "onPause");

        super.onPause();

        if (mSession != null) {
            mSession.onPause();
        }

    }

    @Override
    protected void onResume() {

        Log.d(LOG_TAG, "onResume");

        super.onResume();

        if (mSession != null) {
            mSession.onResume();
        }
    }

    private void initializeSession(String apiKey, String sessionId, String token) {

        Log.d(LOG_TAG, "Initializing Session");

        mSession = new Session.Builder(this, apiKey, sessionId).build();
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(token);
    }

    private void sendMessage() {

        Log.d(LOG_TAG, "Send Message");

        SignalMessage signal = new SignalMessage(mMessageEditTextView.getText().toString());
        mSession.sendSignal(SIGNAL_TYPE, signal.getMessageText());

        mMessageEditTextView.setText("");

    }

    private void showMessage(String messageData, boolean remote) {

        Log.d(LOG_TAG, "Show Message");

        SignalMessage message = new SignalMessage(messageData, remote);
        mMessageHistory.add(message);
    }

    private void logOpenTokError(OpentokError opentokError) {

        Log.e(LOG_TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(LOG_TAG, "Error Code: " + opentokError.getErrorCode().name());
    }

    /* Session Listener methods */

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");
        mMessageEditTextView.setEnabled(true);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Signal Listener methods */

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {

        boolean remote = !connection.equals(mSession.getConnection());
        if (type != null && type.equals(SIGNAL_TYPE)) {
            showMessage(data, remote);
        }
    }

    /* Web Service Coordinator delegate methods */

    @Override
    public void onSessionConnectionDataReady(String apiKey, String sessionId, String token) {

        Log.d(LOG_TAG, "ApiKey: "+apiKey + " SessionId: "+ sessionId + " Token: "+token);

        initializeSession(apiKey, sessionId, token);
    }

    @Override
    public void onWebServiceCoordinatorError(Exception error) {
        showConfigError("Web Service error", error.getMessage());
    }

    /* alert dialogue for errors */

    private void showConfigError(String alertTitle, final String errorMessage) {

        Log.e(LOG_TAG, "Error " + alertTitle + ": " + errorMessage);
        new AlertDialog.Builder(this)
                .setTitle(alertTitle)
                .setMessage(errorMessage)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
