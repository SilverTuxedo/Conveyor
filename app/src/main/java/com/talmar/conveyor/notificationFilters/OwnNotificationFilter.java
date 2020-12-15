package com.talmar.conveyor.notificationFilters;

import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.talmar.conveyor.BuildConfig;

/**
 * This filters out notifications sent by this app itself.
 */
public class OwnNotificationFilter implements INotificationFilter {
    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        return sbn.getPackageName().equals(BuildConfig.APPLICATION_ID);
    }
}
