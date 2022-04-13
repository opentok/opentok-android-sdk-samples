//package com.tokbox.sample.basicvoipcall;
//
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.media.RingtoneManager;
//import android.net.Uri;
//import android.os.Build;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.core.app.NotificationCompat;
//
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.messaging.FirebaseMessaging;
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
//public class OTFireBaseMessagingService extends FirebaseMessagingService {
//
//    private static final String TAG = "OTFireBaseMessaging";
//    private String TOKEN;
//
//    public String getTOKEN() {
//        return TOKEN;
//    }
//
//    public OTFireBaseMessagingService() {
//        super();
//    }
//
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        Log.d(TAG, "onMessageReceived()" +  remoteMessage);
//        super.onMessageReceived(remoteMessage);
//
//        // There are two types of messages data messages and notification messages. Data messages
//        // are handled
//        // here in onMessageReceived whether the app is in the foreground or background. Data
//        // messages are the type
//        // traditionally used with GCM. Notification messages are only received here in
//        // onMessageReceived when the app
//        // is in the foreground. When the app is in the background an automatically generated
//        // notification is displayed.
//        // When the user taps on the notification they are returned to the app. Messages
//        // containing both notification
//        // and data payloads are treated as notification messages. The Firebase console always
//        // sends notification
//        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
//        // [END_EXCLUDE]
//
//        // TODO(developer): Handle FCM messages here.
//        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d(TAG, "From: " + remoteMessage.getFrom());
//
//        // Check if message contains a data payload.
//        if (remoteMessage.getData().size() > 0) {
//            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
//
//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use WorkManager.
//                //scheduleJob();
//            } else {
//                // Handle message within 10 seconds
//                //handleNow();
//            }
//
//        }
//
//        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
//        }
//
//        // Also if you intend on generating your own notifications as a result of a received FCM
//        // message, here is where that should be initiated. See sendNotification method below.
//
//    }
//
//    @Override
//    public void onMessageSent(@NonNull String msgId) {
//        Log.d(TAG, "onMessageSent()");
//        super.onMessageSent(msgId);
//    }
//
//    @Override
//    public void onNewToken(@NonNull String token) {
//        super.onNewToken(token);
//        Log.d(TAG, "onNewToken()");
//        TOKEN = token;
//    }
//
//    /**
//     * Schedule async work using WorkManager.
//     */
//    private void scheduleJob() {
//        /*
//        [START dispatch_job]
//        /OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
//                .build();
//        WorkManager.getInstance(this).beginWith(work).enqueue();
//        // [END dispatch_job]
//         */
//    }
//
//    /**
//     * Handle time allotted to BroadcastReceivers.
//     */
//    private void handleNow() {
//        Log.d(TAG, "Short lived task is done.");
//    }
//
//    /**
//     * Persist token to third-party servers.
//     *
//     * Modify this method to associate the user's FCM registration token with any
//     * server-side account maintained by your application.
//     *
//     * @param token The new token.
//     */
//    private void sendRegistrationToServer(String token) {
//        // TODO: Implement this method to send token to your app server.
//    }
//
//    /**
//     * Create and show a simple notification containing the received FCM message.
//     *
//     * @param messageBody FCM message body received.
//     */
//    private void sendNotification(String messageBody) {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                PendingIntent.FLAG_ONE_SHOT);
//
//        String channelId = getString(R.string.default_notification_channel_id);
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this, channelId)
//                        //.setSmallIcon(R.drawable.ic_stat_ic_notification)
//                        .setContentTitle(getString(R.string.fcm_message))
//                        .setContentText(messageBody)
//                        .setAutoCancel(true)
//                        .setSound(defaultSoundUri)
//                        .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Since android Oreo notification channel is needed.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelId,
//                    "Channel human readable title",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
//    }
//}
