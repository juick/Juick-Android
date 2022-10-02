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
package com.juick.android

import com.juick.android.Utils.eventsFactory
import okhttp3.OkHttpClient
import com.here.oksse.OkSse
import com.here.oksse.ServerSentEvent
import android.util.Log
import com.juick.api.model.Post
import com.juick.App
import com.juick.BuildConfig
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class NotificationManager {
    private var es: OkHttpClient? = null
    fun onResume() {
        if (es != null) return
        es = eventsFactory!!
            .readTimeout(0, TimeUnit.SECONDS).build()
        val request = Request.Builder()
            .url(BuildConfig.EVENTS_ENDPOINT)
            .build()
        val sse = OkSse(es)
        sse.newServerSentEvent(request, object : ServerSentEvent.Listener {
            override fun onOpen(sse: ServerSentEvent, response: Response) {
                Log.d(TAG, "Event listener opened")
            }

            override fun onMessage(
                sse: ServerSentEvent,
                id: String?,
                event: String,
                message: String
            ) {
                Log.d(TAG, "event received: $event")
                if (event == "msg") {
                    try {
                        val reply: Post =
                            App.instance.jsonMapper.readValue(message, Post::class.java)
                        App.instance.newMessage.value = reply
                    } catch (e: IOException) {
                        Log.d(TAG, "JSON exception: " + e.message)
                    }
                }
            }

            override fun onComment(sse: ServerSentEvent, comment: String) {}
            override fun onRetryTime(sse: ServerSentEvent, milliseconds: Long): Boolean {
                return true
            }

            override fun onRetryError(
                sse: ServerSentEvent,
                throwable: Throwable,
                response: Response?
            ): Boolean {
                return true
            }

            override fun onClosed(sse: ServerSentEvent) {}
            override fun onPreRetry(sse: ServerSentEvent, originalRequest: Request): Request {
                return originalRequest
            }
        })
    }

    companion object {
        private val TAG = NotificationManager::class.java.simpleName
    }
}