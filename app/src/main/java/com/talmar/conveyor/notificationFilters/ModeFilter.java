package com.talmar.conveyor.notificationFilters;

import android.app.Notification;
import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.talmar.conveyor.EchoingMode;
import com.talmar.conveyor.components.Conversation;


/**
 * Filters notifications according to the current echoing mode.
 */
public class ModeFilter implements INotificationFilter, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String MODE_KEY_NAME = "mode";

    private EchoingMode m_echoingMode;

    public ModeFilter(SharedPreferences sharedPreferences) {
        m_echoingMode = getModeFromPreferences(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {
        if (EchoingMode.ALL == m_echoingMode) {
            return false;
        }

        Notification notification = sbn.getNotification();
        String notificationTemplate = notification.extras.getString(Notification.EXTRA_TEMPLATE);
        boolean isMessagingStyle = Notification.MessagingStyle.class.getName().equals(notificationTemplate);

        if (!isMessagingStyle) {
            // This is not a messaging-style notification, meaning that we can't determine if it's
            // a group or a DM. Ignore it.
            return true;
        }

        Conversation conversation = new Conversation(sbn.getNotification());
        boolean isGroupMessage = conversation.getAuthors().size() > 1 || conversation.hasTitle();

        boolean isAllowed = (
                isGroupMessage && m_echoingMode == EchoingMode.GROUPS_ONLY
                || !isGroupMessage && m_echoingMode == EchoingMode.DIRECT_MESSAGES_ONLY);

        return !isAllowed;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!key.equals(MODE_KEY_NAME)) {
            return;
        }

        m_echoingMode = getModeFromPreferences(sharedPreferences);
    }

    private static EchoingMode getModeFromPreferences(SharedPreferences preferences) {
        try {
            return EchoingMode.valueOf(preferences.getString(MODE_KEY_NAME, EchoingMode.ALL.name()));
        } catch (IllegalArgumentException e) {
            return EchoingMode.ALL;
        }
    }
}
