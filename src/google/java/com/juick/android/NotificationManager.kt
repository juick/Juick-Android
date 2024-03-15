/*
 * Copyright (C) 2008-2024, Juick
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
import android.text.TextUtils
import android.util.Log
import com.juick.R
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.juick.android.Utils.updateToken
import com.juick.android.service.FirebaseReceiverService

class NotificationManager {
    init {
        val context: Context = App.instance
        if (!TextUtils.isEmpty(context.getString(R.string.gcm_defaultSenderId))) {
            if (GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
            ) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(FirebaseReceiverService.TAG, "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result

                    // Log and toast
                    Log.d(FirebaseReceiverService.TAG, "Token received: $token")
                    updateToken("fcm", token)
                })
            }
        }
    }

    fun onResume() {}
}