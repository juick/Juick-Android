/*
 * Copyright (C) 2008-2020, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.agconnect.AGConnectOptionsBuilder;
import com.huawei.hms.aaid.HmsInstanceId;

public class NotificationManager {
    public NotificationManager(Context context) {
        // get token
        new Thread() {
            @Override
            public void run() {
                try {
                    // read from agconnect-services.json
                    String appId = new AGConnectOptionsBuilder().build(context).getString("client/app_id");
                    String pushtoken = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
                    if (!TextUtils.isEmpty(pushtoken)) {
                        Log.i("HMS", "get token:" + pushtoken);

                    }
                } catch (Exception e) {
                    Log.i("HMS", "getToken failed, " + e);

                }
            }
        }.start();
    }
    public void onResume() {

    }
}
