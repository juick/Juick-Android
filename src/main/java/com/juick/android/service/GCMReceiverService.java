package com.juick.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.bluelinelabs.logansquare.LoganSquare;
import com.juick.R;
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
        try {
            Post jmsg = LoganSquare.parse(msg, Post.class);
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

            Intent i = new Intent(this, MainActivity.class);
            i.setAction(MainActivity.PUSH_ACTION);
            i.putExtra(MainActivity.ARG_UNAME, jmsg.user.uname);
            i.putExtra(MainActivity.ARG_UID, jmsg.user.uid);
            if (jmsg.mid == 0) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(GCM_EVENT_ACTION).putExtra("message", msg));
                i.putExtra(MainActivity.PUSH_ACTION_SHOW_PM, true);
            } else {
                i.putExtra(MainActivity.PUSH_ACTION_SHOW_THREAD, true);
            }
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
            notification.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true).setWhen(0)
                    .setContentIntent(contentIntent)
                    .setDefaults(Notification.DEFAULT_LIGHTS
                            | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification.build());
        } catch (Exception e) {
            Log.e("GCMIntentService", "GCM message error", e);
        }
    }
}
