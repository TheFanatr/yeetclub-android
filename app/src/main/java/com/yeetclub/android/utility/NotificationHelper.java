package com.yeetclub.android.utility;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yeetclub.android.R;
import com.yeetclub.android.activity.MainActivity;
import com.yeetclub.android.enums.NotificationType;
import com.yeetclub.android.parse.ParseConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the center to send notifications.
 *
 * @author shuklaalok7
 * @since 5/12/2016
 */
public class NotificationHelper {

    public static final String NOTIFICATION_GROUP = "com.yitter.summary_notification";

    private static final String TAG = NotificationHelper.class.getSimpleName();
    private static final int SUMMARY_NOTIFICATION_ID = 80012;

    public static NotificationHelper instance = new NotificationHelper();

    private NotificationHelper() {
    }

    /**
     * @param context
     * @param notification
     * @return
     */
    public int showNotification(Context context, android.app.Notification notification) {
        int id = (int) System.currentTimeMillis();
        return showNotification(context, id, notification);
    }

    /**
     * @param context
     * @param notificationId
     * @param notification
     * @return
     */
    public int showNotification(Context context, int notificationId, android.app.Notification notification) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);

        return notificationId;
    }

    /**
     * @param context The context needed to show notification
     * @param builder NotificationBuilder
     * @return notificationId so that caller can update the notification later
     */
    public int showNotification(Context context, NotificationType group, NotificationCompat.Builder builder) {
        int time = ((Long) System.currentTimeMillis()).intValue();
        return this.showNotification(context, time, group, builder);
    }

    /**
     * @param context        The context needed to show notification
     * @param notificationId If you want to give the notification a specific notificationId to be able to update it later
     * @param builder        NotificationBuilder
     * @return notificationId so that caller can update the notification later
     */
    public int showNotification(Context context, int notificationId, NotificationType group, NotificationCompat.Builder builder) {
        if (group == null) {
            Log.w(TAG, "Group of the notification is not set");
        } else {
            builder.setGroup(group.getKey()).setGroupSummary(false);
        }

        showNotification(context, notificationId, builder.build());

//        if (group != null && group.isSummaryAvailable()) {
//            showSummaryNotification(context, createNotification(context, ));
//            notificationManager.notify(group.getSummaryNotificationId(), group.getSummaryNotification(context));
//        }
        return notificationId;
    }

    /**
     * @param userId The recipient
     */
    public void sendPushNotification(String userId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        ParseCloud.callFunctionInBackground("pushFunction", params, (result, e) -> {
            if (e == null) {
                Log.d(TAG, "Push notification successfully sent.");
            } else {
                Log.d(TAG, "Push notification could not be sent.");
            }
        });
    }

    /**
     * @param context
     */
    public void showSummaryNotification(final Context context) {
        if (context == null) {
            return;
        }

        ParseQuery<ParseObject> query1 = ParseQuery.getQuery(ParseConstants.CLASS_NOTIFICATIONS);
        ParseQuery<ParseObject> query2 = ParseQuery.getQuery(ParseConstants.CLASS_NOTIFICATIONS);

        List<ParseQuery<ParseObject>> orList = new ArrayList<>();
        query1.whereDoesNotExist(ParseConstants.KEY_READ_STATE);
        query2.whereEqualTo(ParseConstants.KEY_READ_STATE, false);
        orList.add(query1);
        orList.add(query2);
        ParseQuery<ParseObject> query = ParseQuery.or(orList);
        query.whereEqualTo(ParseConstants.KEY_RECIPIENT_ID, ParseUser.getCurrentUser().getObjectId());
        query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);

        query.findInBackground((notifications, e) -> {
            if (e == null && notifications != null && !notifications.isEmpty()) {
                // We have got notifications
                showSummaryNotification(context, notifications);
            }
        });
    }

    /**
     * @param context
     * @param notifications
     */
    private void showSummaryNotification(final Context context, List<ParseObject> notifications) {
        android.app.Notification notification = createNotification(context, notifications);
        showNotification(context, SUMMARY_NOTIFICATION_ID, notification);
    }

    /**
     * To be used by {@link #showSummaryNotification(Context)} or {@link #showSummaryNotification(Context, List)}
     *
     * @param context Context to create notification
     * @return A summary notification created with given parameters and for current group
     */
    private android.app.Notification createNotification(Context context, List<ParseObject> notifications) {
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                R.mipmap.ic_launcher);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                .setSummaryText("Yeet Club");

        for (ParseObject notification : notifications) {
            style.addLine(notification.getString(ParseConstants.KEY_SENDER_NAME) + notification.getString(ParseConstants.KEY_NOTIFICATION_TEXT));
        }

        // Instantiate summaryNotification
        return new NotificationCompat.Builder(context)
                .setContentTitle(notifications.size() + " new interactions")
                .setSmallIcon(R.drawable.ic_stat_ic_no_notifications)
                .setLargeIcon(largeIcon)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0))
                .setStyle(style)
                .setAutoCancel(true)
                .setGroup(NOTIFICATION_GROUP).setGroupSummary(true).build();
    }

}
