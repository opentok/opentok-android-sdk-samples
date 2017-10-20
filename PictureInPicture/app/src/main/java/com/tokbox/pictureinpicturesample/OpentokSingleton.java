package com.tokbox.pictureinpicturesample;

import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Subscriber;

/**
 * Created by rpc on 23/08/2017.
 */

public class OpentokSingleton {
    private static final OpentokSingleton instance = new OpentokSingleton();

    Session session;
    Publisher publisher;

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    Subscriber subscriber;

    public boolean isSessionConnected() {
        return sessionConnected;
    }

    public void setSessionConnected(boolean sessionConnected) {
        this.sessionConnected = sessionConnected;
    }

    boolean sessionConnected;

    private OpentokSingleton() {

    }

    public static OpentokSingleton getInstance() {
        return instance;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
}
