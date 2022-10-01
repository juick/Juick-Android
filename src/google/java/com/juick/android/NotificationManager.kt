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

import com.juick.android.Utils.updateFCMToken
import android.content.Context
import com.juick.App
import android.text.TextUtils
import com.juick.R
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.messaging.FirebaseMessaging

class NotificationManager {
    init {
        val context: Context = App.instance
        if (!TextUtils.isEmpty(context.getString(R.string.gcm_defaultSenderId))) {
            if (GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
            ) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    updateFCMToken(token)
                }
            }
        }
    }

    fun onResume() {}
}