package com.talmar.conveyor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.talmar.conveyor.components.Conversation;
import com.talmar.conveyor.components.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

public class NotificationEchoing {
    private static final String TAG = NotificationEchoing.class.getSimpleName();
    private static Random random = new Random();

    private static final String ECHO_NOTIFICATIONS_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".echo";
    private static final String ECHO_NOTIFICATIONS_GROUP = "echo_group";
    private static final int ECHO_NOTIFICATION_ICON = R.drawable.ic_echo;
    private static final int GROUP_NOTIFICATION_ID = 0;

    /**
     * Creates the notification channel for echo notification (when the API level supports it).
     *
     * @param context A context for creating the channel.
     */
    private static void createEchoNotificationsChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Echo notifications";
            String description = "Notifications that echo notifications from other apps.";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(ECHO_NOTIFICATIONS_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Creates a notification that best echos the given notification.
     *
     * @param context      The context for the notification object.
     * @param notification The notification to echo.
     * @return The echo notification.
     */
    public static Notification createEchoNotification(Context context, @NonNull Notification notification) {
        createEchoNotificationsChannel(context);
        String notificationTemplate = notification.extras.getString(Notification.EXTRA_TEMPLATE);

        boolean isMessagingStyle = Notification.MessagingStyle.class.getName().equals(notificationTemplate);
        if (isMessagingStyle) {
            return echoMessagingStyleNotification(context, notification);
        } else {
            return echoSimpleNotification(context, notification);
        }
    }

    /**
     * Creates an echo notification by simply reading the title and text of a notification.
     * <p>
     * For complex notifications, like conversations, this may yield inaccurate results.
     *
     * @param context      The context for the created echo notification.
     * @param notification The notification to base the echo notification on.
     * @return The echo notification.
     * @see #echoMessagingStyleNotification
     */
    private static Notification echoSimpleNotification(Context context, @NonNull Notification notification) {
        String title = "";
        Object titleObject = notification.extras.get(Notification.EXTRA_TITLE);
        if (null != titleObject) {
            title = titleObject.toString();
        }

        String text = "";
        Object textObject = notification.extras.get(Notification.EXTRA_TEXT);
        if (null != textObject) {
            text = textObject.toString();
        }

        NotificationCompat.Builder builder = getEchoNotificationBuilder(context, title, text)
                .setExtras(notification.extras);

        return builder.build();
    }

    /**
     * Creates an echo notification for a notification based on {@link Notification.MessagingStyle}.
     * <p>
     * Note that the order of messages in the echo notification is reversed, so new messages appear
     * first.
     *
     * @param context      The context for the created echo notification.
     * @param notification The notification to base the echo notification on.
     * @return The echo notification.
     */
    private static Notification echoMessagingStyleNotification(Context context, @NonNull Notification notification) {
        Conversation conversation = new Conversation(notification);

        ArrayList<String> messageLines = new ArrayList<>();
        for (Message message : conversation.getGroupedMessages()) {
            String messageText;
            // If this conversation has a title, we should put the authors' names before the message
            // itself, since the user will not know who wrote it.
            if (conversation.hasTitle()) {
                messageText = String.format("%s: %s", message.authorName, message.text);
            } else {
                messageText = message.text;
            }
            messageLines.add(messageText);
        }

        String title;
        if (conversation.hasTitle()) {
            title = conversation.getTitle();
        } else {
            // If there is no title, use the authors as a title.
            title = String.join(", ", conversation.getAuthors());
        }

        // We reverse the line order so new messages appear first.
        Collections.reverse(messageLines);
        String notificationText = String.join("\n", messageLines);

        return getEchoNotificationBuilder(context, title, notificationText).build();
    }

    /**
     * @param context A context for creating the builder.
     * @param title   The title of the notification.
     * @param text    The contents of the notification.
     * @return A notification builder for echo notifications, already set with the basic properties.
     */
    private static NotificationCompat.Builder getEchoNotificationBuilder(Context context, String title, String text) {
        // When this notification is clicked, open the app.
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        return new NotificationCompat.Builder(context, ECHO_NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(ECHO_NOTIFICATION_ICON)
                .setGroup(ECHO_NOTIFICATIONS_GROUP)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
    }

    /**
     * Sends an echo notification for the given status bar notification.
     * <p>
     * The echo notification is internally tracked, so if the same SBN gets passed to this function
     * again the first notification sent will be updated accordingly.
     * <p>
     * To remove the notification, use {@link #removeEchoNotification}.
     *
     * @param sbn The relevant status bar notification.
     */
    public static void sendEchoNotification(Context context, StatusBarNotification sbn) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        String sbnKey = sbn.getKey();
        Notification echoNotification = createEchoNotification(context, sbn.getNotification());

        Map<String, Integer> notificationMap = AppSingleton.getInstance(context).getNotificationIdMap();
        // We may already have sent an echo notification for this SBN, so reuse the ID to update the
        // notification we've previously sent.
        Integer echoNotificationId = notificationMap.get(sbnKey);
        if (null == echoNotificationId) {
            // This notification is new to us.
            echoNotificationId = random.nextInt();
            notificationMap.put(sbnKey, echoNotificationId);
        }

        notificationManager.notify(echoNotificationId, echoNotification);
    }

    /**
     * Removes the echo notification sent by {@link #sendEchoNotification} for the given status bar
     * notification.
     * <p>
     * If no echo notification was sent, this method has no effect.
     *
     * @param sbn The relevant status bar notification.
     * @return true if a notification was removed.
     */
    public static boolean removeEchoNotification(Context context, StatusBarNotification sbn) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        String sbnKey = sbn.getKey();

        Map<String, Integer> notificationMap = AppSingleton.getInstance(context).getNotificationIdMap();
        Integer echoNotificationId = notificationMap.get(sbnKey);
        if (null != echoNotificationId) {
            notificationManager.cancel(echoNotificationId);
            notificationMap.remove(sbnKey);
            return true;
        }
        // The sbn notification did not have an echo notification. We can simply ignore.
        Log.d(TAG, "Notification did not have an echo notification: " + sbnKey);
        return false;
    }

    /**
     * Clears all echo notifications sent.
     *
     * @param context A context.
     */
    public static void removeAllEchoNotifications(Context context) {
        Map<String, Integer> notificationMap = AppSingleton.getInstance(context).getNotificationIdMap();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        synchronized (notificationMap) {
            for (Integer id : notificationMap.values()) {
                notificationManager.cancel(id);
            }
            notificationMap.clear();
        }
    }

    /**
     * Sends a notification meant to group echo notifications.
     * This is necessary for Android to be able to group notifications together, otherwise all echo
     * notifications appear separately in the status bar and notification drawer.
     * <p>
     * Since the grouping notification always uses the same ID, this function can be called many
     * times without issue.
     */
    public static void sendGroupingNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationEchoing.ECHO_NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(NotificationEchoing.ECHO_NOTIFICATION_ICON)
                .setGroup(NotificationEchoing.ECHO_NOTIFICATIONS_GROUP)
                .setGroupSummary(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(GROUP_NOTIFICATION_ID, builder.build());
    }

    /**
     * Removes a previously sent grouping notification.
     *
     * @see #sendGroupingNotification
     */
    public static void removeGroupingNotification(NotificationManagerCompat notificationManager) {
        notificationManager.cancel(GROUP_NOTIFICATION_ID);
    }

    /**
     * @param sbn A status bar notification
     * @return true if the notification is an echo notification.
     */
    public static boolean isEchoNotification(StatusBarNotification sbn) {
        return sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)
                && sbn.getNotification().getGroup().equals(ECHO_NOTIFICATIONS_GROUP)
                && 0 == (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY);
    }
}
