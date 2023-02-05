/*
 * Copyright (C) 2008-2022, Juick
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
package com.juick.android

import android.content.Context
import com.juick.App
import java.lang.Thread
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import android.text.TextUtils
import android.util.Log
import java.lang.Exception

class NotificationManager {
    init {
        val context: Context = App.instance
        // get token
        object : Thread() {
            override fun run() {
                try {
                    // read from agconnect-services.json
                    val appId = AGConnectOptionsBuilder().build(context).getString("client/app_id")
                    val pushtoken = HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                    if (!TextUtils.isEmpty(pushtoken)) {
                        Log.i("HMS", "get token:$pushtoken")
                        Utils.updateToken("hcm", pushtoken)
                    }
                } catch (e: Exception) {
                    Log.i("HMS", "getToken failed, $e")
                }
            }
        }.start()
    }

    fun onResume() {}
}