package com.talmar.conveyor.notificationFilters;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

/**
 * Filters out notifications that don't have a title and text.
 */
public class NullContentFilter implements INotificationFilter {

    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        return notification.extras.get(Notification.EXTRA_TITLE) == null
                && notification.extras.get(Notification.EXTRA_TEXT) == null;
    }
}
