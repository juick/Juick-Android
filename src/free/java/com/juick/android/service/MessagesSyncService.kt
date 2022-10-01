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
package com.juick.android.service

import android.accounts.Account
import android.app.Service
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.juick.App
import com.juick.R
import com.juick.api.model.Post
import com.juick.api.model.User

class MessagesSyncService : Service() {
    private class MessagesSyncAdapter(private val appContext: Context) :
        AbstractThreadedSyncAdapter(appContext, true) {
        override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult
        ) {
            val me = App.instance.me.value
            if (me!!.unreadCount > 0) {
                val user = User(0, "Juick")
                val announcement = Post()
                announcement.user = user
                announcement.body = context.getString(R.string.unread_discussions)
                try {
                    val messageData: String =
                        App.instance.jsonMapper.writeValueAsString(announcement)
                    App.instance.notificationSender?.showNotification(messageData)
                } catch (e: JsonProcessingException) {
                    Log.w(this.javaClass.simpleName, "JSON error", e)
                }
            }
        }
    }

    private var messagesSyncAdapter: MessagesSyncAdapter? = null
    override fun onCreate() {
        messagesSyncAdapter = MessagesSyncAdapter(
            applicationContext
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return messagesSyncAdapter!!.syncAdapterBinder
    }
}