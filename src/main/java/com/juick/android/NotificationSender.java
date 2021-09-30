/*
 * Copyright (C) 2008-2021, Juick
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

package com.juick.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bumptech.glide.request.FutureTarget;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.api.GlideApp;
import com.juick.api.model.Post;

import java.util.concurrent.ExecutionException;

public class NotificationSender {
    private static String channelId;

    private static NotificationManager notificationManager =
            (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

    private static Handler handler = new Handler(Looper.getMainLooper());

    public NotificationSender(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            channelId = context.getString(R.string.default_notification_channel_id);
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId,
                        context.getString(R.string.Juick),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
            }
            channel.setDescription(context.getString(R.string.juick_notifications));
        }
    }
    public void showNotification(final String msgStr) {
        try {
            final Post jmsg = App.getInstance().getJsonMapper().readValue(msgStr, Post.class);
            if (jmsg.isService()) {
                handler.post(() -> notificationManager.cancel(String.valueOf(getId(jmsg)), 0));
            } else {
                String title = "@" + jmsg.getUser().getUname();
                if (!jmsg.getTags().isEmpty()) {
                    title += ": " + jmsg.getTagsString();
                }
                String body;
                if (TextUtils.isEmpty(jmsg.getBody())) {
                    body = "sent an image";
                } else {
                    if (jmsg.getBody().length() > 64) {
                        body = jmsg.getBody().substring(0, 60) + "...";
                    } else {
                        body = jmsg.getBody();
                    }
                }

                PendingIntent contentIntent = PendingIntent.getActivity(App.getInstance(),
                        getId(jmsg), createNewEventIntent(msgStr), PendingIntent.FLAG_UPDATE_CURRENT);
                final NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(App.getInstance(), channelId);
                notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true).setWhen(0)
                        .setContentIntent(contentIntent)
                        .setGroup("messages")
                        .setColor(App.getInstance().getResources().getColor(R.color.colorAccent))
                        .setGroupSummary(true);

                notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
                notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(jmsg.getBody()));
                if (jmsg.getUser().getUid() > 0) {
                    notificationBuilder.addAction(Build.VERSION.SDK_INT <= 19 ?
                                    R.drawable.ic_ab_reply2 : R.drawable.ic_ab_reply,
                            App.getInstance().getString(R.string.reply), PendingIntent.getActivity(App.getInstance(),
                                    getId(jmsg), createNewEventIntent(msgStr),
                                    PendingIntent.FLAG_UPDATE_CURRENT));
                }
                FutureTarget<Bitmap> avatarBitmap = GlideApp.with(App.getInstance()).asBitmap()
                        .load(jmsg.getUser().getAvatar())
                        .fallback(R.drawable.av_96)
                        .placeholder(R.drawable.av_96)
                        .centerCrop().submit();
                try {
                    Bitmap avatar = avatarBitmap.get();
                    notificationBuilder.setLargeIcon(avatar);
                } catch (ExecutionException | InterruptedException e) {
                    Log.w(NotificationSender.class.getSimpleName(), "Avatar was not loaded", e);
                }
                if (Build.VERSION.SDK_INT < 26) {
                    notificationBuilder.setDefaults(~(Notification.DEFAULT_LIGHTS
                            | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND));
                }
                notify(jmsg, notificationBuilder);
            }
        } catch (Exception e) {
            Log.e(NotificationSender.class.getSimpleName(), "GCM message error", e);
        }
    }

    private void notify(Post jmsg, NotificationCompat.Builder notificationBuilder) {
        notificationManager.notify(String.valueOf(getId(jmsg)), 0, notificationBuilder.build());
    }

    private Integer getId(Post jmsg) {
        return jmsg.getMid() != 0 ? jmsg.getMid() : jmsg.getUser().getUid();
    }

    public Intent createNewEventIntent(String jmsg) {
        Intent intent = new Intent(App.getInstance(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(BuildConfig.INTENT_NEW_EVENT_ACTION);
        intent.putExtra(App.getInstance().getString(R.string.notification_extra), jmsg);
        return intent;
    }
}
