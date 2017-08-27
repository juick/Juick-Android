package com.juick.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.bluelinelabs.logansquare.LoganSquare;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.juick.App;
import com.juick.R;
import com.juick.api.RestClient;
import com.juick.api.model.Post;
import com.juick.android.MainActivity;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by vt on 03/12/15.
 */
public class GCMReceiverService extends GcmListenerService {

    public final static String GCM_EVENT_ACTION = GCMReceiverService.class.getName() + "_GCM_EVENT_ACTION";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        String msg = data.getString("message");
        Log.d("GCMReceiverService", "onMessageReceived " + data.toString());
        showNotification(msg);
    }

    public static void showNotification(final String msgStr) {
        try {
            final Post jmsg = LoganSquare.parse(msgStr, Post.class);
            String title = "@" + jmsg.user.uname;
            if (!jmsg.tags.isEmpty()) {
                title += ": " + jmsg.tags;
            }
            String body;
            if (jmsg.body.length() > 64) {
                body = jmsg.body.substring(0, 60) + "...";
            } else {
                body = jmsg.body;
            }

            PendingIntent contentIntent = PendingIntent.getActivity(App.getInstance(), 0, getIntent(msgStr, jmsg), PendingIntent.FLAG_UPDATE_CURRENT);
            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(App.getInstance());
            notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true).setWhen(0)
                    .setContentIntent(contentIntent)
                    .setGroup("messages")
                    .setGroupSummary(true)
                    .setDefaults(Notification.DEFAULT_LIGHTS
                            | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(jmsg.body));

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Glide.with(App.getInstance())
                            .load(RestClient.getBaseUrl() + "a/" + jmsg.user.uid + ".png")
                            .asBitmap()
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    notificationBuilder.setLargeIcon(resource);
                                    notificationBuilder.setDefaults(~(Notification.DEFAULT_LIGHTS
                                            | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND));
                                    GCMReceiverService.notify(jmsg, notificationBuilder);
                                }
                            });

                    notificationBuilder.addAction(Build.VERSION.SDK_INT <= 19 ? R.drawable.ic_ab_reply2 : R.drawable.ic_ab_reply,
                            App.getInstance().getString(R.string.reply), PendingIntent.getActivity(App.getInstance(), 2, getIntent(msgStr, jmsg), PendingIntent.FLAG_UPDATE_CURRENT));
                    GCMReceiverService.notify(jmsg, notificationBuilder);
                }
            });

        } catch (Exception e) {
            Log.e("GCMIntentService", "GCM message error", e);
        }
    }

    private static void notify(Post jmsg, NotificationCompat.Builder notificationBuilder) {
        ((NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(jmsg.mid != 0 ? jmsg.mid : jmsg.user.uid, notificationBuilder.build());
    }

    public static Intent getIntent(String msgStr, Post jmsg) {
        Intent intent = new Intent(App.getInstance(), MainActivity.class);
        intent.setAction(MainActivity.PUSH_ACTION);
        intent.putExtra(MainActivity.ARG_UNAME, jmsg.user.uname);
        intent.putExtra(MainActivity.ARG_UID, jmsg.user.uid);
        if (jmsg.mid == 0) {
            LocalBroadcastManager.getInstance(App.getInstance()).sendBroadcast(new Intent(GCM_EVENT_ACTION).putExtra("message", msgStr));
            intent.putExtra(MainActivity.PUSH_ACTION_SHOW_PM, true);
        } else {
            intent.putExtra(MainActivity.ARG_MID, jmsg.mid);
            intent.putExtra(MainActivity.PUSH_ACTION_SHOW_THREAD, true);
        }
        return intent;
    }
}
