package com.opentok.android.demo.opentoksamples;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.demo.config.OpenTokConfig;
import com.opentok.android.demo.services.ClearNotificationService;
import com.opentok.android.ui.textchat.ChatMessage;

import com.opentok.android.ui.textchat.widget.TextChatFragment;

import java.util.ArrayList;

public class TextChatActivity extends FragmentActivity implements Session.SignalListener, Session.SessionListener, TextChatFragment.TextChatListener {

    private static final String LOGTAG = "demo-text-chat";
    private static final String SIGNAL_TYPE = "TextChat";

    private Session mSession;

    private ProgressBar mLoadingBar;

    private boolean resumeHasRun = false;

    private boolean mIsBound = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;

    private RelativeLayout mTextChatViewContainer;
    private TextChatFragment mTextChatFragment;

    private FragmentTransaction mFragmentTransaction;

    private boolean msgError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOGTAG, "ONCREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_textchat_activity);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mLoadingBar = (ProgressBar) findViewById(R.id.load_spinner);
        mLoadingBar.setVisibility(View.VISIBLE);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        sessionConnect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();
        }

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setContentText(getResources().getString(R.string.notification))
                .setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

        Intent notificationIntent = new Intent(this, HelloWorldActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        if (mConnection == null) {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    ((ClearNotificationService.ClearBinder) binder).service.startService(new Intent(TextChatActivity.this, ClearNotificationService.class));
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.notify(ClearNotificationService.NOTIFICATION_ID, mNotifyBuilder.build());
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mConnection = null;
                }

            };
        }

        if (!mIsBound) {
            bindService(new Intent(TextChatActivity.this,
                            ClearNotificationService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;
            startService(notificationIntent);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (!resumeHasRun) {
            resumeHasRun = true;
            return;
        } else {
            if (mSession != null) {
                mSession.onResume();
            }
        }
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        if (isFinishing()) {
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            if (mSession != null) {
                mSession.disconnect();
            }
        }
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mSession != null) {
            mSession.disconnect();
        }

        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mSession != null) {
            mSession.disconnect();
        }

        super.onBackPressed();
    }

    private void sessionConnect() {

        if (mSession == null) {
            mSession = new Session(TextChatActivity.this,
                    OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
            mSession.setSignalListener(this);
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        }
    }

    private void loadTextChatFragment(){

        int containerId = R.id.fragment_textchat_container;
        mFragmentTransaction = getFragmentManager().beginTransaction();
        mTextChatFragment = (TextChatFragment)this.getFragmentManager().findFragmentByTag("TextChatFragment");

        if (mTextChatFragment == null)
        {
            Log.d(LOGTAG, "loadTextChatFragment() - mTextChatFragment is null");
            mTextChatFragment = new TextChatFragment();
            Log.d(LOGTAG, "loadTextChatFragment() - mTextChatFragment after constructor");

            mFragmentTransaction.add(containerId, mTextChatFragment, "TextChatFragment").commit();
            Log.d(LOGTAG, "loadTextChatFragment() - mTextChatFragment add");

            mTextChatFragment.setTextChatListener(this);
            mTextChatFragment.setMaxTextLength(1050);
        }
        else
        {
            Log.d(LOGTAG, "loadTextChatFragment() - mTextChatFragment is not null");
        }


    }

    @Override
    public boolean onMessageReadyToSend(String msgStr) {
        Log.d(LOGTAG, "TextChat listener: onMessageReadyToSend: " +msgStr);

        if (mSession != null) {
            mSession.sendSignal(SIGNAL_TYPE, msgStr);
        }
        //to indicate to the text-chat component if the message is valid and it is ready to be sent
        return msgError;
    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {
        Log.d(LOGTAG, "onSignalReceived. Type: "+ type + " data: "+data);
        ChatMessage msg;
        if (!connection.getConnectionId().equals(mSession.getConnection().getConnectionId())) {
            //message is from other participant --> the status of the ChatMessage is RECEIVED_MESSAGE.
            //Alias for this participant will be the 5 first digits of its connection
            //
            msg = new ChatMessage(connection.getConnectionId().substring(1, 5), data, ChatMessage.MessageStatus.RECEIVED_MESSAGE);
        }
        else {
            //message is from me --> the status of the ChatMessage is SENT_MESSAGE.
            msg = new ChatMessage("me", data, ChatMessage.MessageStatus.SENT_MESSAGE);
        }

        //Add the new ChatMessage to the text-chat component
        mTextChatFragment.addMessage(msg);
    }

    @Override
    public void onConnected(Session session) {
        Log.d(LOGTAG, "Session is connected");

        mLoadingBar.setVisibility(View.GONE);
        //loading text-chat ui component
        loadTextChatFragment();
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(LOGTAG, "Session is disconnected");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(LOGTAG, "Session error. OpenTokError: "+opentokError.getErrorCode() + " - "+ opentokError.getMessage());
        OpentokError.ErrorCode errorCode = opentokError.getErrorCode();
        msgError = true;
    }


    @Override
    public void onStreamReceived(Session session, Stream stream) {
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
    }

}


