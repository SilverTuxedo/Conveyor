package com.talmar.conveyor;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppSingleton {
    @SuppressLint("StaticFieldLeak")
    private static AppSingleton s_instance;
    private Context m_context;
    private Map<String, Integer> m_notificationIdMap;
    private AtomicBoolean m_userPresent;

    private AppSingleton(Context context) {
        m_context = context;
        m_notificationIdMap = getNotificationIdMap();
        m_userPresent = new AtomicBoolean(false);
    }

    public static synchronized AppSingleton getInstance(Context context) {
        if (null == s_instance) {
            s_instance = new AppSingleton(context.getApplicationContext());
        }
        return s_instance;
    }

    public Map<String, Integer> getNotificationIdMap() {
        if (null == m_notificationIdMap) {
            m_notificationIdMap = Collections.synchronizedMap(new HashMap<>());
        }
        return m_notificationIdMap;
    }

    public boolean isUserPresent() {
        return m_userPresent.get();
    }

    public void setUserPresent(boolean present) {
        m_userPresent.set(present);
    }
}
