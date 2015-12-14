package com.example.volkerpetersen.sailingrace;
/**
 * Created by Volker Petersen - November 2015.
 *
 * Activity to update and display the current Setting preferences
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Switch;

import java.util.Hashtable;

public class SailingRacePreferences extends PreferenceActivity {
    public static Hashtable<String, String[]> prefs = new Hashtable<String, String[]>();
    public EditTextPreference summaryValue;

    public static int FetchPreferenceValue(String key, Context who) {
        final String LOG_TAG = SailingRacePreferences.class.getSimpleName();
        // the HashTable prefs contains for each key (which equals the Shared Preferences key)
        // a String[] with the 2 items [Preference Summary, Preference Default Value]
        String[] item;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(who);
        if (prefs.isEmpty()) {
            initializePreferencesHashTable();
        }
        item = prefs.get(key);
        if (item != null) {
            if (key.equals("key_Windex") || key.equals("key_Warning")) {
                boolean b = SP.getBoolean(key, false);
                return (b) ? 1 : 0;
            } else {
                String value = SP.getString(key, item[1]);
                return Integer.parseInt(value);
            }
        } else {
            Log.d(LOG_TAG, "Key error, empty array on " + key);
            return 0;
        }
    }

    public static void initializePreferencesHashTable() {
        // Hashtable with values for the TextPreferences
        String[] preference_keys = new String[] {"key_StartSequence", "key_ScreenUpdates",
                "key_GPSUpdateTime", "key_GPSUpdateDistance", "key_TackAngle", "key_GybeAngle",
                "key_history", "key_Windex", "key_Warning", "key_RaceClass"};
        String[] pref_summary = new String[] {"Subsequent Class starts every INT minutes",
                "Screen Updates every INT seconds", "New location every INT milli-seconds",
                "New location every INT meters", "Target Tack Angle INT°", "Target Gybe Angle INT°",
                "Keep INT old positions", "", "", ""};
        String[] pref_defaults = new String[] {"3", "2", "500", "2", "40", "30", "30", "4", "0", "1"};

        // initialize the HashTable prefs containing for each key (which equals the Shared Preferences key)
        // a String[] with the 2 items [Preference Summary, Preference Default Value]
        for (int i=0; i<preference_keys.length; i++) {
            String[] item = {pref_summary[i], pref_defaults[i]};
            prefs.put(preference_keys[i], item);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializePreferencesHashTable();
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public class MyPreferenceFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        public final String LOG_TAG = MyPreferenceFragment.class.getSimpleName();

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(com.example.volkerpetersen.sailingrace.R.xml.preferences);
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            UpdatePreferenceValues(SP);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            UpdatePreferenceValues(sharedPreferences);
        }

        public void UpdatePreferenceValues(SharedPreferences SP) {
            String[] item;
            final boolean polarsEnabled = SP.getBoolean("key_Polars", true);
            final boolean windexEnabled = SP.getBoolean("key_Windex", true);

            getPreferenceScreen().findPreference("key_Polars").setEnabled(windexEnabled);
            getPreferenceScreen().findPreference("key_TackAngle").setEnabled(!windexEnabled);
            getPreferenceScreen().findPreference("key_GybeAngle").setEnabled(!windexEnabled);

            if (windexEnabled && !polarsEnabled) {
                getPreferenceScreen().findPreference("key_TackAngle").setEnabled(true);
                getPreferenceScreen().findPreference("key_GybeAngle").setEnabled(true);
            }

            for(String key: prefs.keySet()){
                item = prefs.get(key);
                if (!item[0].equals("")) {
                    summaryValue = (EditTextPreference) findPreference(key);
                    String summary = item[0].replace("INT", SP.getString(key, item[1]));
                    summaryValue.setSummary(summary);
                }
            }
        }

        @Override
        public void onResume() {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            super.onResume();
        }

        @Override
        public void onPause() {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
    }

}
