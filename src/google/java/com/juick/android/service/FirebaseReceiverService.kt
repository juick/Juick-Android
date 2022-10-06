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
package com.juick.android.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.juick.App
import com.juick.R
import com.juick.android.Utils.updateFCMToken
import com.juick.api.model.Post
import java.io.IOException

/**
 * Created by vt on 03/12/15.
 */
class FirebaseReceiverService : FirebaseMessagingService() {
    private val TAG = FirebaseReceiverService::class.java.simpleName
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val msg = data[App.instance.getString(R.string.notification_extra)]
        Log.d(TAG, "onMessageReceived $data")
        val isForegroundMessage = message.notification != null
        if (isForegroundMessage) {
            Log.d(TAG, "Message received in foreground")
            try {
                val reply: Post = App.instance.jsonMapper.readValue(msg, Post::class.java)
                if (!reply.isService) {
                    App.instance.messages.value = listOf(reply)
                }
            } catch (e: IOException) {
                Log.d(TAG, "JSON exception: " + e.message)
            }
        } else {
            App.instance.notificationSender.showNotification(msg)
        }
    }

    override fun onNewToken(refreshedToken: String) {
        Log.d(TAG, "Refreshed token: $refreshedToken")
        updateFCMToken(refreshedToken)
    }
}