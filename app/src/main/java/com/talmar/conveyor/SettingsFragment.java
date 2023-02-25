package com.talmar.conveyor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.talmar.conveyor.services.NotificationService;
import com.talmar.conveyor.services.UserPresenceService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final String GARMIN_CONNECT_PACKAGE_NAME = "com.garmin.android.apps.connectmobile";

    // Set of packages that will appear first in the app list.
    private static final HashSet<CharSequence> PREFERRED_PACKAGES = new HashSet<>(Arrays.asList(
            "org.telegram.messenger", // Telegram
            "org.thunderdog.challegram", // Telegram X
            "org.telegram.plus", // Plus Messenger
            "com.whatsapp", // WhatsApp
            "com.whatsapp.w4b", // WhatsApp Business
            "com.facebook.orca", // Facebook Messenger
            "com.instagram.android", // Instagram
            "com.google.android.apps.messaging", // Messages (by Google)
            "com.discord", // Discord
            "org.thoughtcrime.securesms")); // Signal

    private SwitchPreferenceCompat m_hasNotificationAccessSwitch;
    private MultiSelectListPreference m_selectedAppsPreference;
    private CheckBoxPreference m_clearNotificationsOnUnlockCheckBox;
    private Preference m_openGarminConnectPreference;
    private DropDownPreference m_modePreference;
    private EditTextPreference m_titles_to_ignore;

    private AlertDialog m_batteryOptimizationDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        m_batteryOptimizationDialog = buildBatteryOptimizationAlertDialog();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        m_hasNotificationAccessSwitch = findPreference("has_notification_access");
        m_selectedAppsPreference = findPreference("selected_apps");
        m_clearNotificationsOnUnlockCheckBox = findPreference("clear_notifications_on_unlock");
        m_openGarminConnectPreference = findPreference("open_garmin_connect");
        m_modePreference = findPreference("mode");
        m_titles_to_ignore = findPreference("titles_to_ignore");

        assert null != m_hasNotificationAccessSwitch;
        assert null != m_selectedAppsPreference;
        assert null != m_clearNotificationsOnUnlockCheckBox;
        assert null != m_openGarminConnectPreference;
        assert null != m_modePreference;
        assert null != m_titles_to_ignore;

        populateAppListPreference(m_selectedAppsPreference);
        pruneUninstalledAppsFromPreference(m_selectedAppsPreference);
        m_selectedAppsPreference.setSummaryProvider(
                (Preference.SummaryProvider<MultiSelectListPreference>) preference ->
                        getString(R.string.selected_apps_indicator_title, preference.getValues().size()));

        m_hasNotificationAccessSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            if (NotificationService.isEnabled(requireContext())) {
                // The user should disable the service in the settings.
                startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else {
                // The user should enable the service. Prompt nicely.
                buildNotificationServiceAlertDialog().show();
            }
            return false;
        });

        m_clearNotificationsOnUnlockCheckBox.setOnPreferenceChangeListener((preference, newValue) -> {
            Boolean enabled = (Boolean) newValue;
            setUserPresenceServiceActive(enabled);
            return true;
        });

        Intent garminConnectLaunchIntent = requireContext().getPackageManager().getLaunchIntentForPackage(GARMIN_CONNECT_PACKAGE_NAME);
        if (null != garminConnectLaunchIntent) {
            m_openGarminConnectPreference.setIntent(garminConnectLaunchIntent);
            m_openGarminConnectPreference.setEnabled(true);
            m_openGarminConnectPreference.setSummary(R.string.garmin_connect_tutorial);
        }

        populateModePreference(m_modePreference);
        m_modePreference.setSummaryProvider(
                (Preference.SummaryProvider<DropDownPreference>) preference -> {
                    EchoingMode mode;
                    try {
                        mode = EchoingMode.valueOf(preference.getValue());
                    } catch (IllegalArgumentException e) {
                        mode = EchoingMode.ALL;
                    }

                    int resource = R.string.mode_all_notifications_description;
                    switch (mode) {
                        case GROUPS_ONLY:
                            resource = R.string.mode_groups_only_description;
                            break;
                        case DIRECT_MESSAGES_ONLY:
                            resource = R.string.mode_direct_only_description;
                            break;
                    }

                    return getString(resource);
                });

        syncPreferencesToSystemState();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncPreferencesToSystemState();
    }

    private void syncPreferencesToSystemState() {
        m_hasNotificationAccessSwitch.setChecked(NotificationService.isEnabled(requireContext()));
        setUserPresenceServiceActive(m_clearNotificationsOnUnlockCheckBox.isChecked());

        String packageName = requireContext().getPackageName();
        PowerManager powerManager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            m_batteryOptimizationDialog.show();
        }
    }

    /**
     * Activates or deactivates the user presence service.
     *
     * @param active whether the service should be activated or deactivated.
     */
    private void setUserPresenceServiceActive(boolean active) {
        Intent serviceIntent = new Intent(requireContext(), UserPresenceService.class);
        if (active) {
            requireContext().startService(serviceIntent);
        } else {
            requireContext().stopService(serviceIntent);
        }
    }

    /**
     * Builds a dialog that explains to the user that the notification listener service must be
     * enabled in the settings, and includes a shortcut to the correct screen.
     *
     * @return An alert dialog which leads to the notification enabling screen.
     */
    private AlertDialog buildNotificationServiceAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setTitle(R.string.notification_access_dialog_title);
        alertDialogBuilder.setMessage(R.string.notification_access_dialog_message);
        alertDialogBuilder.setPositiveButton(R.string.notification_access_dialog_positive,
                (dialog, id) -> {
                    dialog.dismiss();
                    startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                });
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                (dialog, id) -> Snackbar.make(
                                SettingsFragment.this.requireView(),
                                R.string.notification_access_missing_warning,
                                Snackbar.LENGTH_LONG)
                        .show());
        return alertDialogBuilder.create();
    }

    /**
     * Builds a dialog that explains to the user that battery optimization needs to be disabled in
     * the settings, and includes a shortcut to the correct screen.
     *
     * @return An alert dialog which leads to the battery optimization screen.
     */
    private AlertDialog buildBatteryOptimizationAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext());
        alertDialogBuilder.setTitle(R.string.disable_optimization_dialog_title);
        alertDialogBuilder.setMessage(R.string.disable_optimization_dialog_message);
        alertDialogBuilder.setPositiveButton(R.string.disable_optimization_dialog_positive,
                (dialog, id) -> startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                (dialog, id) -> Snackbar.make(
                        SettingsFragment.this.requireView(),
                        R.string.battery_optimization_active_warning,
                        Snackbar.LENGTH_LONG)
                        .show());
        return alertDialogBuilder.create();
    }

    /**
     * Sets the entries and values of a multi-select setting for selecting from installed apps.
     *
     * @param preference The preference to set entries for.
     */
    private void populateAppListPreference(MultiSelectListPreference preference) {
        Pair<CharSequence[], CharSequence[]> labelsAndNames = Applications.getSortedInstalledAppsLabelAndPackageName(requireContext().getPackageManager(), PREFERRED_PACKAGES);
        preference.setEntries(labelsAndNames.first);
        preference.setEntryValues(labelsAndNames.second);
    }

    /**
     * Removes uninstalled apps from an app preference's selected values.
     *
     * @param preference The preference to modify.
     */
    private void pruneUninstalledAppsFromPreference(MultiSelectListPreference preference) {
        final Set<CharSequence> installedApps = Applications.getInstalledApps(requireContext().getPackageManager());

        Set<String> filteredValues = preference.getValues().stream().filter(installedApps::contains).collect(Collectors.toSet());
        preference.setValues(filteredValues);
    }

    private void populateModePreference(DropDownPreference preference) {
        CharSequence[] entries = new CharSequence[3];
        entries[0] = getString(R.string.mode_all_notifications);
        entries[1] = getString(R.string.mode_groups_only);
        entries[2] = getString(R.string.mode_direct_only);
        preference.setEntryValues(Stream.of(EchoingMode.values()).map(EchoingMode::name).toArray(CharSequence[]::new));
        preference.setEntries(entries);

        if (preference.getValue() == null) {
            preference.setValue(EchoingMode.ALL.name());
        }
    }
}
