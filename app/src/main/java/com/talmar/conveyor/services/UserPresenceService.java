package com.talmar.conveyor.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class UserPresenceService extends Service {
    private static final String TAG = UserPresenceService.class.getSimpleName();

    UserPresenceReceiver receiver = new UserPresenceReceiver();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);

        Log.d(TAG, "User presence service started");
        receiver.onReceive(this, new Intent(Intent.ACTION_USER_PRESENT));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.d(TAG, "User presence service destroyed");
    }
}
