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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.api.model.Post
import java.util.concurrent.ExecutionException

class NotificationSender(context: Context) {
    private val notificationManager =
        App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val handler = Handler(Looper.getMainLooper())

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            channelId = context.getString(R.string.default_notification_channel_id)
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.Juick),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.enableLights(true)
                channel.enableVibration(true)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(channel)
            }
            channel.description = context.getString(R.string.juick_notifications)
        }
    }

    fun showNotification(msgStr: String?) {
        try {
            val jmsg = App.getInstance().jsonMapper.readValue(msgStr, Post::class.java)
            if (jmsg.isService) {
                handler.post { notificationManager.cancel(getId(jmsg).toString(), 0) }
            } else {
                var title = "@${jmsg.user.uname}"
                if (jmsg.tags.isNotEmpty()) {
                    title = "$title: ${jmsg.tagsString}"
                }
                val body = if (TextUtils.isEmpty(jmsg.body)) {
                    "sent an image"
                } else {
                    if (jmsg.body.length > 64) {
                        jmsg.body.substring(0, 60) + "..."
                    } else {
                        jmsg.body
                    }
                }
                val contentIntent = PendingIntent.getActivity(
                    App.getInstance(),
                    getId(jmsg), createNewEventIntent(msgStr), PendingIntent.FLAG_UPDATE_CURRENT
                )
                val notificationBuilder = NotificationCompat.Builder(App.getInstance(), channelId!!)
                notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true).setWhen(0)
                    .setContentIntent(contentIntent)
                    .setGroup("messages")
                    .setColor(ContextCompat.getColor(App.getInstance(), R.color.colorAccent))
                    .setGroupSummary(true)
                notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(jmsg.body))
                if (jmsg.user.uid > 0) {
                    notificationBuilder.addAction(
                        if (Build.VERSION.SDK_INT <= 19) R.drawable.ic_ab_reply2 else R.drawable.ic_ab_reply,
                        App.getInstance().getString(R.string.reply), PendingIntent.getActivity(
                            App.getInstance(),
                            getId(jmsg), createNewEventIntent(msgStr),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                }
                val avatarBitmap = Glide.with(App.getInstance()).asBitmap()
                    .load(jmsg.user.avatar)
                    .fallback(R.drawable.av_96)
                    .placeholder(R.drawable.av_96)
                    .centerCrop().submit()
                try {
                    val avatar = avatarBitmap.get()
                    notificationBuilder.setLargeIcon(avatar)
                } catch (e: ExecutionException) {
                    Log.w(NotificationSender::class.java.simpleName, "Avatar was not loaded", e)
                } catch (e: InterruptedException) {
                    Log.w(NotificationSender::class.java.simpleName, "Avatar was not loaded", e)
                }
                if (Build.VERSION.SDK_INT < 26) {
                    notificationBuilder.setDefaults(
                        (Notification.DEFAULT_LIGHTS
                                or Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND).inv()
                    )
                }
                notify(jmsg, notificationBuilder)
            }
        } catch (e: Exception) {
            Log.e(NotificationSender::class.java.simpleName, "GCM message error", e)
        }
    }

    private fun notify(jmsg: Post, notificationBuilder: NotificationCompat.Builder) {
        notificationManager.notify(getId(jmsg).toString(), 0, notificationBuilder.build())
    }

    private fun getId(jmsg: Post): Int {
        return if (jmsg.mid != 0) jmsg.mid else jmsg.user.uid
    }

    private fun createNewEventIntent(jmsg: String?): Intent {
        val intent = Intent(App.getInstance(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.action = BuildConfig.INTENT_NEW_EVENT_ACTION
        intent.putExtra(App.getInstance().getString(R.string.notification_extra), jmsg)
        return intent
    }

    companion object {
        private var channelId: String? = null
    }
}