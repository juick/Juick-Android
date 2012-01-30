/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.juick.R;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ugnich
 */
public class CheckUpdatesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String jsonStr = Utils.getJSON(context, "http://api.juick.com/notifications");
        if (jsonStr != null && jsonStr.length() > 4) {
            try {
                JSONObject json = new JSONObject(jsonStr);

                int messages = json.getInt("messages");
                String str = "New messages: " + messages;

                Intent i = new Intent(context, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = new Notification(R.drawable.ic_notification, str, System.currentTimeMillis());
                notification.setLatestEventInfo(context.getApplicationContext(), str, str, contentIntent);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.defaults |= Notification.DEFAULT_LIGHTS;
                //notification.defaults |= Notification.DEFAULT_VIBRATE;
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);

            } catch (JSONException e) {
                Log.e("CheckUpdatesReceiver", e.toString());
            }
        }
    }
}
