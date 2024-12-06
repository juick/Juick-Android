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
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.widget.util.loadImage
import com.juick.api.model.Post
import kotlinx.coroutines.runBlocking

object NotificationSender {
    private val handler = Handler(Looper.getMainLooper())

    fun showNotification(context: Context, msgStr: String?) {
        val notificationManager =
            context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                channel.description = context.getString(R.string.juick_notifications)
            }
        }
        try {
            val jmsg = App.instance.jsonMapper.decodeFromString<Post>(msgStr ?: "")
            val notificationId = getId(jmsg)
            if (jmsg.isService) {
                Log.d(TAG, "Notification cleared: $notificationId")
                handler.post { notificationManager.cancel(notificationId.toString(), 0) }
                val updatedMessages = App.instance.messages.value.toMutableList().apply {
                    removeAll { if (jmsg.mid != 0) { it.mid == notificationId } else {it.user.uid == notificationId } }
                }.toList()
                App.instance.messages.value = updatedMessages
            } else {
                if (!App.AppLifecycleManager.isInForeground) {
                    Log.d(TAG, "Notification added: $notificationId")
                    var title = "@${jmsg.user.uname}"
                    if (jmsg.tags.isNotEmpty()) {
                        title = "$title: ${jmsg.tagsString}"
                    }
                    val body = if (TextUtils.isEmpty(jmsg.text)) {
                        "sent an image"
                    } else {
                        if (jmsg.text.length > 64) {
                            jmsg.text.substring(0, 60) + "..."
                        } else {
                            jmsg.text
                        }
                    }
                    val contentIntent = PendingIntent.getActivity(
                        context,
                        getId(jmsg),
                        createNewEventIntent(context, msgStr),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val notificationBuilder = NotificationCompat.Builder(context, channelId)
                    notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true).setWhen(0)
                        .setContentIntent(contentIntent)
                        .setGroup("messages")
                        .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                        .setGroupSummary(true)
                    notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    notificationBuilder.setStyle(
                        NotificationCompat.BigTextStyle().bigText(jmsg.text)
                    )
                    if (jmsg.user.uid > 0) {
                        notificationBuilder.addAction(
                            R.drawable.ic_ab_reply,
                            context.getString(R.string.reply), PendingIntent.getActivity(
                                context,
                                getId(jmsg), createNewEventIntent(context, msgStr),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    }
                    if (Build.VERSION.SDK_INT < 26) {
                        notificationBuilder.setDefaults(
                            (Notification.DEFAULT_LIGHTS
                                    or Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND).inv()
                        )
                    }
                    runBlocking {
                        notificationBuilder.setLargeIcon(loadImage(jmsg.user.avatar))
                        notificationManager.notify(getId(jmsg).toString(), 0, notificationBuilder.build())
                    }
                } else {
                    Log.d(TAG, "Notification silenced: $notificationId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Notification error", e)
        }
    }

    private fun getId(jmsg: Post): Int {
        return if (jmsg.mid != 0) jmsg.mid else jmsg.user.uid
    }

    private fun createNewEventIntent(context: Context, jmsg: String?): Intent {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.action = BuildConfig.INTENT_NEW_EVENT_ACTION
        intent.putExtra(context.getString(R.string.notification_extra), jmsg)
        return intent
    }
    private lateinit var channelId: String
    const val TAG = "Notification"
}