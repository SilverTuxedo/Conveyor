package com.talmar.conveyor.notificationFilters;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.talmar.conveyor.AppSingleton;
import com.talmar.conveyor.services.ServiceUtils;
import com.talmar.conveyor.services.UserPresenceService;

/**
 * Filters out notifications that were sent while the user is present.
 */
public class UserPresenceFilter implements INotificationFilter, SharedPreferences.OnSharedPreferenceChangeListener {
    private static String PREFERENCES_KEY = "no_notifications_while_unlocked";
    private Context m_context;
    private boolean m_ignoreWhenUserPresent;

    public UserPresenceFilter(Context context, SharedPreferences sharedPreferences) {
        m_context = context;
        m_ignoreWhenUserPresent = sharedPreferences.getBoolean(PREFERENCES_KEY, false);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        ActivityManager manager = (ActivityManager) m_context.getSystemService(Context.ACTIVITY_SERVICE);
        if (!ServiceUtils.isServiceRunning(manager, UserPresenceService.class)) {
            // Information about user presence is not reliable.
            return false;
        }

        if (!m_ignoreWhenUserPresent) {
            // The setting is not enabled, don't do anything.
            return false;
        }
        // If the user is present, we shouldn't send the notification.
        return AppSingleton.getInstance(m_context).isUserPresent();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREFERENCES_KEY)) {
            m_ignoreWhenUserPresent = sharedPreferences.getBoolean(PREFERENCES_KEY, false);
        }
    }
}
