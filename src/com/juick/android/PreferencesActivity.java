/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import com.juick.R;

/**
 *
 * @author ugnich
 */
public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private ListPreference prefRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        prefRefresh = (ListPreference) getPreferenceScreen().findPreference("refresh");
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateSummaries();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummaries();
        if (key.equals("refresh")) {
            MainActivity.startCheckUpdates(this);
        }
    }

    private void updateSummaries() {
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        String refresh = sp.getString("refresh", "5");
        String vals[] = getResources().getStringArray(R.array.prefsRefreshIntervalValues);
        int id = 1;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].equals(refresh)) {
                id = i;
            }
        }
        prefRefresh.setSummary(getResources().getStringArray(R.array.prefsRefreshIntervalTitles)[id]);
    }
}
