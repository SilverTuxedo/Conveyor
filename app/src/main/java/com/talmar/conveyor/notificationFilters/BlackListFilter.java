package com.talmar.conveyor.notificationFilters;

import android.app.Notification;
import android.content.SharedPreferences;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import com.talmar.conveyor.components.Conversation;

import java.util.regex.Pattern;


/**
 * Filters notifications according to the current echoing mode.
 */
public class BlackListFilter implements INotificationFilter, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TITLES_TO_IGNORE_KEY_NAME = "titles_to_ignore";

    private String[] m_titles_to_ignore;

    public BlackListFilter(SharedPreferences sharedPreferences) {
        m_titles_to_ignore = getTitlesToIgnoreFromPreferences(sharedPreferences);

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean shouldIgnoreNotification(@NonNull StatusBarNotification sbn) {

        Notification notification = sbn.getNotification();
        String notificationTemplate = notification.extras.getString(Notification.EXTRA_TEMPLATE);
        boolean isMessagingStyle = Notification.MessagingStyle.class.getName().equals(notificationTemplate);

        if (!isMessagingStyle) {
            // This is not a messaging-style notification, meaning that we can't determine if it's
            // a group or a DM. Ignore it.
            return true;
        }

        Conversation conversation = new Conversation(sbn.getNotification());

        if (!conversation.hasTitle()) {
            // no title, can't blacklist. skip the check
            return false;
        }

        String title = conversation.getTitle();

        for (String pattern :
                m_titles_to_ignore) {
            if (Pattern.matches(pattern, title)) {
                // title found in list to ignore. so we ignore
                return true;
            }
        }

        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!key.equals(TITLES_TO_IGNORE_KEY_NAME)) {
            return;
        }

        m_titles_to_ignore = getTitlesToIgnoreFromPreferences(sharedPreferences);
    }

    private static String[] getTitlesToIgnoreFromPreferences(SharedPreferences preferences) {
        return preferences.getString(TITLES_TO_IGNORE_KEY_NAME, "").split("\n");
    }
}
