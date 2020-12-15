package com.talmar.conveyor.notificationFilters;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

/**
 * Filters out summary notifications - they are designed to group notifications
 * together, and should not be conveyed to the user.
 */
public class SummaryNotificationFilter implements INotificationFilter {
    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        return 0 != (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY);
    }
}
