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
package com.juick.android.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.juick.App
import com.juick.R
import com.juick.android.Utils.updateToken
import com.juick.api.model.Post
import kotlinx.serialization.SerializationException

/**
 * Created by vt on 03/12/15.
 */
class FirebaseReceiverService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val msg = data[App.instance.getString(R.string.notification_extra)] ?: ""
        Log.d(TAG, "onMessageReceived $data")
        try {
            val reply: Post = App.instance.jsonMapper.decodeFromString<Post>(msg)
            if (!reply.isService) {
                App.instance.messages.value = listOf(reply)
            }
            App.instance.notificationSender.showNotification(msg)
        } catch (e: SerializationException) {
            Log.d(TAG, "JSON exception: " + e.message)
        }
    }

    override fun onNewToken(refreshedToken: String) {
        Log.d(TAG, "Refreshed token: $refreshedToken")
        updateToken("fcm", refreshedToken)
    }

    companion object {
        const val TAG = "FCM"
    }
}