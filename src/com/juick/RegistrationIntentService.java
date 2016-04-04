/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
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
package com.juick;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.juick.android.Utils;

import java.net.URLEncoder;

/**
 *
 * @author Ugnich Anton
 */
public class RegistrationIntentService extends IntentService {

    public final static String SENDER_ID = "661985690018";

    public RegistrationIntentService() {
        super(RegistrationIntentService.class.getSimpleName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            final String regId = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i("Juick.GCM", String.format("token: %s", regId));
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String prefRegId = sp.getString("gcm_regid", null);
            if (prefRegId != null && !regId.equals(prefRegId)) {
                Utils.getJSON(this,
                        String.format("https://api.juick.com/android/unregister?regid=%s",
                                URLEncoder.encode(prefRegId, "UTF-8")));
            }
            if (regId != null && !regId.equals(prefRegId)) {
                String res = Utils.getJSON(this,
                        String.format("https://api.juick.com/android/register?regid=%s",
                                URLEncoder.encode(regId, "UTF-8")));
                if (res != null) {
                    SharedPreferences.Editor spe = sp.edit();
                    spe.putString("gcm_regid", regId);
                    spe.commit();
                }
            }
        } catch (Exception e) {
            Log.e("Juick.GCM", "registration error", e);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("REGISTRATION_COMPLETE"));
    }
}
