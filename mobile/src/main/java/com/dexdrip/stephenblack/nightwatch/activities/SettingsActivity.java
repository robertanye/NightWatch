package com.dexdrip.stephenblack.nightwatch.activities;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;


import com.dexdrip.stephenblack.nightwatch.services.DataCollectionService;
import com.dexdrip.stephenblack.nightwatch.watch.PebbleSync;
import com.dexdrip.stephenblack.nightwatch.R;


public class SettingsActivity extends AppCompatActivity  {
    public static final String MENU_NAME = "Settings";
    public static SharedPreferences prefs;

    @Override
    protected void onResume(){
        super.onResume();
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.navDrawer);
        AllPrefsFragment prefsFragment = new AllPrefsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, prefsFragment).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }
    public static class AllPrefsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_license);

            //Notifications
            addPreferencesFromResource(R.xml.pref_notifications);
            //bindPreferenceSummaryToValue(findPreference("other_alerts_sound"));
            addPreferencesFromResource(R.xml.pref_graph);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("maxBgYAxis"));
            addPreferencesFromResource(R.xml.pref_rates);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("other_alerts_snooze"));
            addPreferencesFromResource(R.xml.pref_data_source);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("highValue"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("lowValue"));
            addPreferencesFromResource(R.xml.pref_bg_notification);
            bindPreferenceSummaryToValue(findPreference("bg_alert_profile"));
            bindPreferenceSummaryToValue(findPreference("falling_bg_val"));
            bindPreferenceSummaryToValue(findPreference("rising_bg_val"));
            addPreferencesFromResource(R.xml.pref_watch_integration);

            final Preference pebbleSync = findPreference("broadcast_to_pebble");

            addPreferencesFromResource(R.xml.pref_errors);

            bindPreferenceSummaryToValue(findPreference("dex_collection_method"));
            bindPreferenceSummaryToValue(findPreference("units"));
            bindPreferenceSummaryToValue(findPreference("dexcom_account_name"));

            final PreferenceCategory dataSource = (PreferenceCategory) findPreference("dataSource");
            final Preference share_poll = findPreference("share_poll");
            final Preference dexcom_account_name = findPreference("dexcom_account_name");
            final Preference dexcom_account_password = findPreference("dexcom_account_password");
            final Preference nightscout_poll = findPreference("nightscout_poll");
            final Preference dex_collection_method = findPreference("dex_collection_method");

            prefs = getPreferenceManager().getSharedPreferences();
            if (!prefs.getBoolean("nightscout_poll", false)) {
                dataSource.removePreference(dex_collection_method);
            }
            if (!prefs.getBoolean("share_poll", false)) {
                dataSource.removePreference(dexcom_account_name);
                dataSource.removePreference(dexcom_account_password);
            }
            share_poll.setOnPreferenceChangeListener((preference, value) -> {
                if ((boolean) value) {
                    dataSource.addPreference(dexcom_account_name);
                    dataSource.addPreference(dexcom_account_password);
                } else {
                    dataSource.removePreference(dexcom_account_name);
                    dataSource.removePreference(dexcom_account_password);
                }
                Context context = preference.getContext();
                context.startService(new Intent(context, DataCollectionService.class));
                return true;
            });

            nightscout_poll.setOnPreferenceChangeListener((preference, value) -> {
                if ((boolean) value) {
                    dataSource.addPreference(dex_collection_method);
                } else {
                    dataSource.removePreference(dex_collection_method);
                }
                Context context = preference.getContext();
                context.startService(new Intent(context, DataCollectionService.class));
                return true;
            });

            Preference.OnPreferenceChangeListener collectionPrefValueListener = (preference, value) -> {
                Context context = preference.getContext();
                context.startService(new Intent(context, DataCollectionService.class));
                return true;
            };

            dexcom_account_name.setOnPreferenceChangeListener(collectionPrefValueListener);
            dexcom_account_password.setOnPreferenceChangeListener(collectionPrefValueListener);
            dex_collection_method.setOnPreferenceChangeListener(collectionPrefValueListener);

            pebbleSync.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = preference.getContext();
                if ((Boolean) newValue) {
                    context.startService(new Intent(context, PebbleSync.class));
                } else {
                    context.stopService(new Intent(context, PebbleSync.class));
                }
                return true;
            });
        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }

    }

    private static Preference.OnPreferenceChangeListener sBindNumericPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();
        if (isNumeric(stringValue)) {
            preference.setSummary(stringValue);
            return true;
        }
        return false;
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

        }
//        else if (preference instanceof ringtonepreference) {
//            if (textutils.isempty(stringvalue)) {
//                preference.setsummary("silent");
//            } else {
//                ringtone ringtone = ringtonemanager.getringtone(
//                        preference.getcontext(), uri.parse(stringvalue));
//
//                if (ringtone == null) {
//                    preference.setsummary(null);
//                } else {
//                    string name = ringtone.gettitle(preference.getcontext());
//                    preference.setsummary(name);
//                }
//            }
       else {
            preference.setSummary(stringValue);
        }
        return true;
    };


    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceSummaryToValueAndEnsureNumeric(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindNumericPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
