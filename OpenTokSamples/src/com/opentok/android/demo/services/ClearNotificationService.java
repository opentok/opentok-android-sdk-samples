package com.opentok.android.demo.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ClearNotificationService extends Service {
	public static final String MY_SERVICE = "com.opentok.android.demo.services.ClearNotificationService";
	public class ClearBinder extends Binder {
		public final Service service;

		public ClearBinder(Service service) {
			this.service = service;
		}
	}
	public static int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	private final IBinder mBinder = new ClearBinder(this);

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}
	@Override
	public void onCreate() {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}
	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

}