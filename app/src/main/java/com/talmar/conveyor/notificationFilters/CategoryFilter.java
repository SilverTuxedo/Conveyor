package com.talmar.conveyor.notificationFilters;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

/**
 * Filters notifications based on their category.
 * <p>
 * Progress notifications - filtered since they tend to spam with updates.
 * Service notifications - not interesting to the average user.
 */
public class CategoryFilter implements INotificationFilter {
    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        String notificationCategory = sbn.getNotification().category;
        if (null == notificationCategory) {
            return false;
        }
        return notificationCategory.equals(Notification.CATEGORY_PROGRESS)
                || notificationCategory.equals(Notification.CATEGORY_SERVICE);
    }
}
