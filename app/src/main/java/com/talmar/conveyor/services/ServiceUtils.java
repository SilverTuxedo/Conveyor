package com.talmar.conveyor.services;

import android.app.ActivityManager;

public class ServiceUtils {
    /**
     * Checks if te service is running. Note: this works only for the app's own services.
     *
     * @param activityManager The activity manager
     * @param serviceClass    The service to check for
     * @return true if the service is running.
     */
    public static boolean isServiceRunning(ActivityManager activityManager, Class<?> serviceClass) {
        //noinspection deprecation - getRunningServices is valid for inspecting the app's own services.
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
