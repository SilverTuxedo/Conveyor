package com.talmar.conveyor.notificationFilters;

import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

public interface INotificationFilter {
    /**
     * @return true if the notification should be ignored.
     */
    boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn);
}
