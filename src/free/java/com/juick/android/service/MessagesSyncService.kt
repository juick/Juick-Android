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
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.R
import com.juick.api.model.Post
import com.juick.api.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MessagesSyncService : LifecycleService() {
    private class MessagesSyncAdapter(private val lifecycleScope: CoroutineScope,
                                      private val appContext: Context) :
        AbstractThreadedSyncAdapter(appContext, true) {
        override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult
        ) {
            lifecycleScope.launch {
                try {
                    val me = App.instance.api.me()
                    if (me.unreadCount > 0) {
                        val user = User(0, "Juick")
                        val announcement = Post()
                        announcement.setUser(user)
                        announcement.setBody(context.getString(R.string.unread_discussions))
                        val messageData = App.instance.jsonMapper.writeValueAsString(announcement)
                        App.instance.notificationSender?.showNotification(messageData)
                    }
                } catch (e: Exception) {
                    Log.w(this.javaClass.simpleName, "Sync error", e)
                }
            }
        }
    }

    private lateinit var messagesSyncAdapter: MessagesSyncAdapter
    override fun onCreate() {
        super.onCreate()
        messagesSyncAdapter = MessagesSyncAdapter(
            lifecycleScope,
            applicationContext
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return messagesSyncAdapter.syncAdapterBinder
    }
}