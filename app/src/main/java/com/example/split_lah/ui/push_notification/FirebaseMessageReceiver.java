package com.example.split_lah.ui.push_notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.split_lah.MainActivity;
import com.example.split_lah.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


// receiving push notifications with firebase
public class FirebaseMessageReceiver extends FirebaseMessagingService{
    private static final String TAG = "FirebaseMessageReceiver";

        // Override onNewToken when a new device token is generated(used by firebase to send msgs)
        @Override
        public void onNewToken(@NonNull String token)
        {
            Log.d(TAG, "Refreshed token: " + token); //logs the new token
        }

        // Override onMessageReceived() method to extract the title and body from the message
        // passed in FCM
        @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
            //if contains custom data like key-value pairs
            if (remoteMessage.getData().isEmpty()) {
                //extract notification title, body and groupId
                String groupId = remoteMessage.getData().get("groupId");
                String title = remoteMessage.getNotification().getTitle();
                String message = remoteMessage.getNotification().getBody();
                showNotification(title, message, groupId); //display the message
            }
            //if its just a basic notification payload
            if (remoteMessage.getNotification() != null) {
                // extract and show notification title and body
                showNotification(
                        remoteMessage.getNotification().getTitle(),
                        remoteMessage.getNotification().getBody(),
                        remoteMessage.getData().get("groupId"));
            }
        }

        // Method to set custom layout for notification
        private RemoteViews getCustomDesign(String title, String message) {
            RemoteViews remoteViews = new RemoteViews(
                    getApplicationContext().getPackageName(),
                    R.layout.push_notification); //use our custom layout file
            remoteViews.setTextViewText(R.id.title, title); //set title text
            remoteViews.setTextViewText(R.id.message, message); //set message text
            return remoteViews;
        }

        // Method to display the notifications
        public void showNotification(String title, String message, String groupId) {
            // Create intent to open the debt relation fragment screen
            Intent intent = new Intent(this, MainActivity.class);
            // assign notification channel id, for oreo and up:
            String channel_id = "Payment Notifications";
            //assign groupKey
            String groupKey = "bill_group_" + groupId;
            // clear other activities on top
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //if there is a groupId
            if (groupId != null) {
                intent.putExtra("groupId", groupId);
            }
            // create pending intent
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            // build notification
            NotificationCompat.Builder builder = new
                    NotificationCompat.Builder(getApplicationContext(), channel_id)
                    .setSmallIcon(R.drawable.small_app_icon) //icon in notification bar
                    .setAutoCancel(true) //close when notification tapped
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 }) // vibration pattern
                    .setOnlyAlertOnce(true) //alert only the first time
                    .setContentIntent(pendingIntent); //open app when tapped

            // custom layout for Android versions 4.1 and above.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                builder = builder.setContent(getCustomDesign(title, message));
            } // fallback for older versions before 4.1
            else {
                builder = builder.setContentTitle(title)
                        .setContentText(message)
                        .setSmallIcon(R.drawable.small_app_icon);
            }
            // toolbox to post, cancel and update notifications
            NotificationManager notificationManager
                    = (NotificationManager)getSystemService( //asking for access to notifications at system level
                    Context.NOTIFICATION_SERVICE); //specifically the notification manager

            // for android 8 and up, create a notification channel (required by system)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel =
                        new NotificationChannel(channel_id, "Payment Notifications",
                                NotificationManager.IMPORTANCE_HIGH); //importance level
                notificationManager.createNotificationChannel(notificationChannel);
            }

//            // Create a summary notification that groups everything
//            NotificationCompat.Builder summaryBuilder =
//                    new NotificationCompat.Builder(getApplicationContext(),channel_id)
//                    .setContentTitle("Split Lah")
//                    .setContentText("You have unpaid bills to settle")
//                    .setSmallIcon(R.drawable.small_app_icon)
//                    .setStyle(new NotificationCompat.InboxStyle()
//                            .addLine(title + ": " + message)
//                            .setSummaryText("Grouped Bill Notifications"))
//                    .setGroup(groupKey)
//                    .setGroupSummary(true);


            //unique id for each notification, can show multiple notifications
            int notificationId = (int) System.currentTimeMillis();

            //shows a notification
            notificationManager.notify(notificationId, builder.build());
            //notificationManager.notify(999, summaryBuilder.build());


        }
}

