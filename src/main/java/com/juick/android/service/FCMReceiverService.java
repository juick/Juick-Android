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
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.juick.App;
import com.juick.R;
import com.juick.android.MainActivity;
import com.juick.android.Utils;
import com.juick.api.GlideApp;
import com.juick.api.RestClient;
import com.juick.api.model.Post;

import java.util.Map;

/**
 * Created by vt on 03/12/15.
 */
public class FCMReceiverService extends FirebaseMessagingService {

    public final static String GCM_EVENT_ACTION = FCMReceiverService.class.getName() + "_GCM_EVENT_ACTION";

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
        NotificationChannel channel =  notificationManager.getNotificationChannel(channelId);
        if (channel == null) {
            channel = new NotificationChannel(channelId,
                    "Juick",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Juick notifications");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String,String> data = message.getData();
        String msg = data.get("message");
        Log.d("FCMReceiverService", "onMessageReceived " + data.toString());
        showNotification(msg);
    }

    public static void showNotification(final String msgStr) {
        try {
            final Post jmsg = RestClient.getJsonMapper().readValue(msgStr, Post.class);
            if (jmsg.isService()) {
                notificationManager.cancel(getId(jmsg));
                return;
            }
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

            PendingIntent contentIntent = PendingIntent.getActivity(App.getInstance(), getId(jmsg), getIntent(msgStr, jmsg), PendingIntent.FLAG_UPDATE_CURRENT);
            final NotificationCompat.Builder notificationBuilder = Build.VERSION.SDK_INT < 26 ?
                    new NotificationCompat.Builder(App.getInstance()) : new NotificationCompat.Builder(App.getInstance(), channelId);
            notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true).setWhen(0)
                    .setContentIntent(contentIntent)
                    .setGroup("messages")
                    .setGroupSummary(true);
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(jmsg.getBody()));

            Handler handler = new Handler(Looper.getMainLooper());
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

                notificationBuilder.addAction(Build.VERSION.SDK_INT <= 19 ? R.drawable.ic_ab_reply2 : R.drawable.ic_ab_reply,
                        App.getInstance().getString(R.string.reply), PendingIntent.getActivity(App.getInstance(), getId(jmsg), getIntent(msgStr, jmsg), PendingIntent.FLAG_UPDATE_CURRENT));
                notify(jmsg, notificationBuilder);
            });

        } catch (Exception e) {
            Log.e("GCMIntentService", "GCM message error", e);
        }
    }

    private static void notify(Post jmsg, NotificationCompat.Builder notificationBuilder) {
        notificationManager.notify(getId(jmsg), notificationBuilder.build());
    }

    private static Integer getId(Post jmsg) {
        return jmsg.getMid() != 0 ? jmsg.getMid() : jmsg.getUser().getUid();
    }

    public static Intent getIntent(String msgStr, Post jmsg) {
        Intent intent = new Intent(App.getInstance(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(MainActivity.PUSH_ACTION);
        intent.putExtra(MainActivity.ARG_UNAME, jmsg.getUser().getUname());
        intent.putExtra(MainActivity.ARG_UID, jmsg.getUser().getUid());
        if (jmsg.getMid() == 0) {
            LocalBroadcastManager.getInstance(App.getInstance()).sendBroadcast(new Intent(GCM_EVENT_ACTION).putExtra("message", msgStr));
            intent.putExtra(MainActivity.PUSH_ACTION_SHOW_PM, true);
        } else {
            intent.putExtra(MainActivity.ARG_MID, jmsg.getMid());
            intent.putExtra(MainActivity.PUSH_ACTION_SHOW_THREAD, true);
        }
        return intent;
    }

    @Override
    public void onNewToken(String refreshedToken) {
        Log.d("FCMReceiverService", "Refreshed token: " + refreshedToken);
        Utils.updateFCMToken(refreshedToken);
    }
}
