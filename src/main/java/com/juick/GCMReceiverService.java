package com.juick;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmListenerService;
import com.juick.android.MainActivity;
import com.juick.android.PMActivity;
import com.juick.android.api.JuickMessage;
import org.json.JSONObject;

/**
 * Created by vt on 03/12/15.
 */
public class GCMReceiverService extends GcmListenerService {

    public final static String GCMEVENTACTION = "com.juick.android.gcm-event";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        String msg = data.getString("message");
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String curactivity = sp.getString("currentactivity", null);

            JSONObject jsonmsg = new JSONObject(msg);
            JuickMessage jmsg = JuickMessage.parseJSON(jsonmsg);
            if (jmsg.MID == 0 && curactivity != null && curactivity.equals("pm-" + jmsg.User.UID)) {
                Intent i = new Intent(GCMEVENTACTION);
                i.putExtra("message", msg);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            } else {
                String title = "@" + jmsg.User.UName;
                if (!jmsg.tags.isEmpty()) {
                    title += ": " + jmsg.getTags();
                }
                String body;
                if (jmsg.Text.length() > 64) {
                    body = jmsg.Text.substring(0, 60) + "...";
                } else {
                    body = jmsg.Text;
                }

                int notifpublic = 3;
                try {
                    notifpublic = Integer.parseInt(sp.getString("notif_public", "3"));
                } catch (Exception e) {
                }

                if (notifpublic > 0) {
                    Intent i;
                    if (jmsg.MID == 0) {
                        i = new Intent(this, PMActivity.class);
                        i.putExtra("uname", jmsg.User.UName);
                        i.putExtra("uid", jmsg.User.UID);
                    } else {
                        i = new Intent(this, MainActivity.class);
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
                }
            }
        } catch (Exception e) {
            Log.e("GCMIntentService", "GCM message error", e);
        }
    }
}
