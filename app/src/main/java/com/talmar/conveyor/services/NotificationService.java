package com.talmar.conveyor.services;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.talmar.conveyor.BuildConfig;
import com.talmar.conveyor.NotificationEchoing;
import com.talmar.conveyor.notificationFilters.BlackListFilter;
import com.talmar.conveyor.notificationFilters.CategoryFilter;
import com.talmar.conveyor.notificationFilters.INotificationFilter;
import com.talmar.conveyor.notificationFilters.ModeFilter;
import com.talmar.conveyor.notificationFilters.NullContentFilter;
import com.talmar.conveyor.notificationFilters.OwnNotificationFilter;
import com.talmar.conveyor.notificationFilters.SelectedAppsFilter;
import com.talmar.conveyor.notificationFilters.SummaryNotificationFilter;
import com.talmar.conveyor.notificationFilters.UserPresenceFilter;

import java.util.Arrays;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = NotificationService.class.getSimpleName();
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    private INotificationFilter[] filters = {};

    /**
     * @param context A context.
     * @return true if the notification service is enabled.
     */
    public static boolean isEnabled(Context context) {
        ComponentName componentName = new ComponentName(context, NotificationService.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        return flat != null && flat.contains(componentName.flattenToString());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        filters = new INotificationFilter[]{
                new OwnNotificationFilter(),
                new SummaryNotificationFilter(),
                new CategoryFilter(),
                new NullContentFilter(),
                new SelectedAppsFilter(sharedPreferences),
                new UserPresenceFilter(this, sharedPreferences),
                new ModeFilter(sharedPreferences),
                new BlackListFilter(sharedPreferences),
        };
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        for (StatusBarNotification existingSbn : getActiveNotifications()) {
            onNotificationPosted(existingSbn);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "Notification posted:");
        logNotificationInformation(sbn);

        for (INotificationFilter filter : filters) {
            if (filter.shouldIgnoreNotification(sbn)) {
                Log.d(TAG, "Ignoring notification due to filter " + filter.getClass().getSimpleName());
                return;
            }
        }

        NotificationEchoing.sendEchoNotification(this, sbn);
        NotificationEchoing.sendGroupingNotification(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed:");
        logNotificationInformation(sbn);

        NotificationEchoing.removeEchoNotification(this, sbn);
        if (sbn.getPackageName().equals(BuildConfig.APPLICATION_ID) && !echoNotificationsExist()) {
            NotificationEchoing.removeGroupingNotification(NotificationManagerCompat.from(this));
        }
    }

    private void logNotificationInformation(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        String message =
                "Package: " + sbn.getPackageName() + "\n" +
                        "Key: " + sbn.getKey() + "\n" +
                        "Template: " + notification.extras.get(Notification.EXTRA_TEMPLATE) + "\n" +
                        "Title: " + notification.extras.get(Notification.EXTRA_TITLE) + "\n" +
                        "Text: " + notification.extras.get(Notification.EXTRA_TEXT);
        Log.d(TAG, message);
    }

    /**
     * @return true if there are echo notifications.
     */
    private boolean echoNotificationsExist() {
        return Arrays.stream(getActiveNotifications()).anyMatch(NotificationEchoing::isEchoNotification);
    }
}
