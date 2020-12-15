package com.talmar.conveyor;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Applications {
    /**
     * @return Pairs of (Application label, Application package name) for all installed apps which
     * can be launched.
     */
    public static List<Pair<CharSequence, CharSequence>> getInstalledAppsLabelAndPackageName(PackageManager packageManager) {
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(0);

        List<Pair<CharSequence, CharSequence>> result = new ArrayList<>();
        for (ApplicationInfo appInfo : packages) {
            if (BuildConfig.APPLICATION_ID.equals(appInfo.packageName)) {
                continue;
            }
            if (null == packageManager.getLaunchIntentForPackage(appInfo.packageName)) {
                continue;
            }

            CharSequence label = appInfo.loadLabel(packageManager);
            result.add(new Pair<>(label, appInfo.packageName));
        }
        return result;
    }

    /**
     * @param packageManager        The package manager.
     * @param preferredPackageNames Packages that should appear first in the list.
     * @return Two arrays where for a given index, the first array contains an installed app's
     * label and the second array contains the installed app's package name - sorted where
     * the preferred packages appear first and the labels are sorted alphabetically.
     * @see #getInstalledAppsLabelAndPackageName
     */
    public static Pair<CharSequence[], CharSequence[]> getSortedInstalledAppsLabelAndPackageName(PackageManager packageManager, Set<CharSequence> preferredPackageNames) {
        List<Pair<CharSequence, CharSequence>> labelAndName = getInstalledAppsLabelAndPackageName(packageManager);
        labelAndName.sort((o1, o2) -> {
            boolean firstIsPreferred = preferredPackageNames.contains(o1.second);
            boolean secondIsPreferred = preferredPackageNames.contains(o2.second);
            if (firstIsPreferred == secondIsPreferred) {
                // Both are preferred equally. Sort by label.
                return o1.first.toString().toLowerCase().compareTo(o2.first.toString().toLowerCase());
            }
            return firstIsPreferred ? -1 : 1;
        });

        return splitApplicationPairs(labelAndName);
    }

    private static Pair<CharSequence[], CharSequence[]> splitApplicationPairs(List<Pair<CharSequence, CharSequence>> labelsAndNames) {
        CharSequence[] labels = new CharSequence[labelsAndNames.size()];
        CharSequence[] names = new CharSequence[labelsAndNames.size()];
        for (int i = 0; i < labelsAndNames.size(); i++) {
            Pair<CharSequence, CharSequence> entry = labelsAndNames.get(i);
            labels[i] = entry.first;
            names[i] = entry.second;
        }
        return new Pair<>(labels, names);
    }

    /**
     * @return A set containing package names of all installed apps which can be launched.
     */
    public static Set<CharSequence> getInstalledApps(PackageManager packageManager) {
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(0);

        Set<CharSequence> result = new HashSet<>();
        for (ApplicationInfo appInfo : packages) {
            if (BuildConfig.APPLICATION_ID.equals(appInfo.packageName)) {
                continue;
            }
            if (null == packageManager.getLaunchIntentForPackage(appInfo.packageName)) {
                continue;
            }
            result.add(appInfo.packageName);
        }
        return result;
    }
}
