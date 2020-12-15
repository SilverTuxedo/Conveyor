package com.talmar.conveyor.notificationFilters;

import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Filters out apps that were not selected in the preferences.
 */
public class SelectedAppsFilter implements INotificationFilter, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String APPS_KEY_NAME = "selected_apps";

    private Set<String> m_wantedPackageNames;

    public SelectedAppsFilter(SharedPreferences sharedPreferences) {
        m_wantedPackageNames = sharedPreferences.getStringSet(APPS_KEY_NAME, new HashSet<>());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        return !m_wantedPackageNames.contains(sbn.getPackageName());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!key.equals(APPS_KEY_NAME)) {
            return;
        }
        m_wantedPackageNames = sharedPreferences.getStringSet(APPS_KEY_NAME, new HashSet<>());
    }
}
