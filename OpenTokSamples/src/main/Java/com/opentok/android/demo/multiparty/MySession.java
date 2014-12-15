package com.opentok.android.demo.multiparty;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import com.opentok.android.Connection;
import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.demo.config.OpenTokConfig;

public class MySession extends Session {

    Context mContext;

    // Interface
    ViewPager mSubscribersViewContainer;
    ViewGroup mPreview;
    TextView mMessageView;
    ScrollView mMessageScroll;

    // Players status
    ArrayList<MySubscriber> mSubscribers = new ArrayList<MySubscriber>();
    HashMap<Stream, MySubscriber> mSubscriberStream = new HashMap<Stream, MySubscriber>();
    HashMap<String, MySubscriber> mSubscriberConnection = new HashMap<String, MySubscriber>();

    PagerAdapter mPagerAdapter = new PagerAdapter() {

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return ((MySubscriber) arg1).getView() == arg0;
        }

        @Override
        public int getCount() {
            return mSubscribers.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(position < mSubscribers.size()) {
                return mSubscribers.get(position).getName();
            } else {
                return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
        	MySubscriber p = mSubscribers.get(position);
            container.addView(p.getView());
            return p;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position,
                Object object) {
            for (MySubscriber p : mSubscribers) {
                if (p == object) {
                    if (!p.getSubscribeToVideo()) {
                        p.setSubscribeToVideo(true);
                    }
                } else {
                    if (p.getSubscribeToVideo()) {
                        p.setSubscribeToVideo(false);
                    }
                }
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        	MySubscriber p = (MySubscriber) object;
            container.removeView(p.getView());
        }

        @Override
        public int getItemPosition(Object object) {
            for(int i=0; i < mSubscribers.size(); i++) {
                if(mSubscribers.get(i) == object) {
                    return i;
                }
            }
            return POSITION_NONE;
        }

    };

    public MySession(Context context) {
        super(context, OpenTokConfig.API_KEY,OpenTokConfig.SESSION_ID);
        this.mContext = context;
    }

    // public methods
    public void setPlayersViewContainer(ViewPager container) {
        this.mSubscribersViewContainer = container;
        this.mSubscribersViewContainer.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();
    }

    public void setMessageView(TextView et, ScrollView scroller) {
        this.mMessageView = et;
        this.mMessageScroll = scroller;
    }

    public void setPreviewView(ViewGroup preview) {
        this.mPreview = preview;
    }

    public void connect() {
        this.connect(OpenTokConfig.TOKEN);
    }

    public void sendChatMessage(String message) {
        sendSignal("chat", message);
        presentMessage("Me", message);
    }

    // callbacks
    @Override
    protected void onConnected() {
        Publisher p = new Publisher(mContext, "MyPublisher");
        publish(p);

        // Add video preview
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mPreview.addView(p.getView(), lp);
        p.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        presentText("Welcome to OpenTok Chat.");
    }

    @Override
    protected void onStreamReceived(Stream stream) {
    	MySubscriber p = new MySubscriber(mContext, stream);

        // we can use connection data to obtain each user id
        p.setUserId(stream.getConnection().getData());

        // Subscribe audio only if we have more than one player
        if(mSubscribers.size() != 0) {
            p.setSubscribeToVideo(false);
        }

        // Subscribe to this player
        this.subscribe(p);

        mSubscribers.add(p);
        mSubscriberStream.put(stream, p);
        mSubscriberConnection.put(stream.getConnection().getConnectionId(), p);
        mPagerAdapter.notifyDataSetChanged();

        presentText("\n" + p.getName() + " has joined the chat");
    }

    @Override
    protected void onStreamDropped(Stream stream) {
    	MySubscriber p = mSubscriberStream.get(stream);
        if (p != null) {
            mSubscribers.remove(p);
            mSubscriberStream.remove(stream);
            mSubscriberConnection.remove(stream.getConnection().getConnectionId());
            mPagerAdapter.notifyDataSetChanged();
            
            presentText("\n" + p.getName() + " has left the chat");
        }
    }

    @Override
    protected void onSignalReceived(String type, String data,
            Connection connection) {
    	
    	if(type != null && "chat".equals(type)) {
    		String mycid = this.getConnection().getConnectionId();
    		String cid = connection.getConnectionId();
    		if (!cid.equals(mycid)) {
            		MySubscriber p = mSubscriberConnection.get(cid);
            		if (p != null) {
            			presentMessage(p.getName(), data);
            		}
            	}
        	}
    }

    private void presentMessage(String who, String message) {
        presentText("\n" + who + ": " + message);
    }

    private void presentText(String message) {
        mMessageView.setText(mMessageView.getText() + message);
        mMessageScroll.post(new Runnable() {
            @Override
            public void run() {
                int totalHeight = mMessageView.getHeight();
                mMessageScroll.smoothScrollTo(0, totalHeight);
            }
        });
    }

}
