package com.tokbox.sample.basicvideochat_connectionservice;

public class VonageConnectionHolder {

    private static VonageConnectionHolder instance;
    private VonageConnection connection;

    private VonageConnectionHolder() {}

    public static synchronized VonageConnectionHolder getInstance() {
        if (instance == null) {
            instance = new VonageConnectionHolder();
        }
        return instance;
    }

    public VonageConnection getConnection() {
        return connection;
    }

    public void setConnection(VonageConnection connection) {
        this.connection = connection;
    }
}