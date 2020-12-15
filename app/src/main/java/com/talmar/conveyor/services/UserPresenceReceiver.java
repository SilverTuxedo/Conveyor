package com.talmar.conveyor.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.talmar.conveyor.AppSingleton;
import com.talmar.conveyor.NotificationEchoing;

public class UserPresenceReceiver extends BroadcastReceiver {
    private static final String TAG = UserPresenceReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null == intent) {
            return;
        }
        String action = intent.getAction();
        if (null == action) {
            return;
        }
        Log.d(TAG, "action: " + action);

        if (action.equals(Intent.ACTION_USER_PRESENT)) {
            AppSingleton.getInstance(context).setUserPresent(true);
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("clear_notifications_on_unlock", false)) {
                NotificationEchoing.removeAllEchoNotifications(context);
            }
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            AppSingleton.getInstance(context).setUserPresent(false);
        }
    }
}
