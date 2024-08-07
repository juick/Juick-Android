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
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.juick.App
import com.juick.R
import com.juick.android.Utils.updateToken
import com.juick.api.model.Post
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.IOException

class HmsReceiverService : HmsMessageService() {
    private val TAG = "HMS"
    override fun onMessageReceived(message: RemoteMessage) {
        val data = App.instance.jsonMapper.parseToJsonElement(message.data)
        val msg = data.jsonObject[App.instance.getString(R.string.notification_extra)]
        Log.d(TAG, "onMessageReceived $data ${message.messageType}")
        try {
            val reply: Post = App.instance.jsonMapper.decodeFromJsonElement(msg!!.jsonObject)
            if (!reply.isService) {
                App.instance.messages.value = listOf(reply)
            }
            App.instance.notificationSender.showNotification(msg.jsonObject.toString())
        } catch (e: IOException) {
            Log.d(TAG, "JSON exception: " + e.message)
        }
    }

    override fun onNewToken(s: String) {
        Log.d(TAG, "Token: $s")
        updateToken("hcm", s)
    }
}