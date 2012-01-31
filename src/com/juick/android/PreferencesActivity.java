/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
 * @author Ugnich Anton
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
