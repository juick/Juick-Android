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

package com.juick.android.service;

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

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.MainActivity;
import com.juick.android.Utils;
import com.juick.api.GlideApp;
import com.juick.api.model.Post;

import java.util.Map;

/**
 * Created by vt on 03/12/15.
 */
public class FCMReceiverService extends FirebaseMessagingService {

    private static String channelId;

    private static NotificationManager notificationManager =
            (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        channelId = getApplicationContext().getString(R.string.default_notification_channel_id);
        NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
        if (channel == null) {
            channel = new NotificationChannel(channelId,
                    getApplicationContext().getString(R.string.Juick),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
        channel.setDescription(getApplicationContext().getString(R.string.juick_notifications));
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> data = message.getData();
        String msg = data.get(App.getInstance().getString(R.string.notification_extra));
        Log.d(FCMReceiverService.class.getSimpleName(), "onMessageReceived " + data.toString());
        boolean isForegroundMessage = message.getNotification() != null;
        if (isForegroundMessage) {
            Log.d(FCMReceiverService.class.getSimpleName(), "Message received in foreground");
            LocalBroadcastManager.getInstance(App.getInstance())
                    .sendBroadcast(new Intent(BuildConfig.INTENT_NEW_EVENT_ACTION)
                            .putExtra(getString(R.string.notification_extra), msg));
        } else {
            showNotification(msg);
        }
    }

    public static void showNotification(final String msgStr) {
        try {
            final Post jmsg = App.getInstance().getJsonMapper().readValue(msgStr, Post.class);
            Handler handler = new Handler(Looper.getMainLooper());
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
                final NotificationCompat.Builder notificationBuilder = Build.VERSION.SDK_INT < 26 ?
                        new NotificationCompat.Builder(App.getInstance()) :
                        new NotificationCompat.Builder(App.getInstance(), channelId);
                notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true).setWhen(0)
                        .setContentIntent(contentIntent)
                        .setGroup("messages")
                        .setGroupSummary(true);
                notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
                notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(jmsg.getBody()));
                handler.post(() -> {
                    GlideApp.with(App.getInstance()).asBitmap()
                            .load(jmsg.getUser().getAvatar())
                            .centerCrop()
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                    notificationBuilder.setLargeIcon(resource);
                                    if (Build.VERSION.SDK_INT < 26) {
                                        notificationBuilder.setDefaults(~(Notification.DEFAULT_LIGHTS
                                                | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND));
                                    }
                                    FCMReceiverService.notify(jmsg, notificationBuilder);
                                }
                            });
                    if (jmsg.getUser().getUid() > 0) {
                        notificationBuilder.addAction(Build.VERSION.SDK_INT <= 19 ?
                                        R.drawable.ic_ab_reply2 : R.drawable.ic_ab_reply,
                                App.getInstance().getString(R.string.reply), PendingIntent.getActivity(App.getInstance(),
                                        getId(jmsg), createNewEventIntent(msgStr),
                                        PendingIntent.FLAG_UPDATE_CURRENT));
                    }
                    notify(jmsg, notificationBuilder);
                });
            }
        } catch (Exception e) {
            Log.e(FCMReceiverService.class.getSimpleName(), "GCM message error", e);
        }
    }

    private static void notify(Post jmsg, NotificationCompat.Builder notificationBuilder) {
        notificationManager.notify(String.valueOf(getId(jmsg)), 0, notificationBuilder.build());
    }

    private static Integer getId(Post jmsg) {
        return jmsg.getMid() != 0 ? jmsg.getMid() : jmsg.getUser().getUid();
    }

    public static Intent createNewEventIntent(String jmsg) {
        Intent intent = new Intent(App.getInstance(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(BuildConfig.INTENT_NEW_EVENT_ACTION);
        intent.putExtra(App.getInstance().getString(R.string.notification_extra), jmsg);
        return intent;
    }

    @Override
    public void onNewToken(@NonNull String refreshedToken) {
        Log.d(FCMReceiverService.class.getSimpleName(), "Refreshed token: " + refreshedToken);
        Utils.updateFCMToken(refreshedToken);
    }
}
