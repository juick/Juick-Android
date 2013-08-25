/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gcm.GCMBaseIntentService;
import com.juick.android.MainActivity;
import com.juick.android.PMActivity;
import com.juick.android.Utils;
import com.juick.android.api.JuickMessage;
import java.net.URLEncoder;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class GCMIntentService extends GCMBaseIntentService {

    public final static String SENDER_ID = "314097120259";
    public final static String GCMEVENTACTION = "com.juick.android.gcm-event";

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String regId) {
        try {
            String res = Utils.getJSON(context, "http://api.juick.com/android/register?regid=" + URLEncoder.encode(regId, "UTF-8"));
            if (res != null) {
                SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(context).edit();
                spe.putString("gcm_regid", regId);
                spe.commit();
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        try {
            Utils.getJSON(context, "http://api.juick.com/android/unregister?regid=" + URLEncoder.encode(regId, "UTF-8"));
            SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(context).edit();
            spe.remove("gcm_regid");
            spe.commit();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        if (intent.hasExtra("message")) {
            String msg = intent.getExtras().getString("message");
            try {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                String curactivity = sp.getString("currentactivity", null);

                JSONObject jsonmsg = new JSONObject(msg);
                JuickMessage jmsg = JuickMessage.parseJSON(jsonmsg);
                if (jmsg.MID == 0 && curactivity != null && curactivity.equals("pm-" + jmsg.User.UID)) {
                    Intent i = new Intent(GCMEVENTACTION);
                    i.putExtra("message", msg);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
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
                            i = new Intent(context, PMActivity.class);
                            i.putExtra("uname", jmsg.User.UName);
                            i.putExtra("uid", jmsg.User.UID);
                        } else {
                            i = new Intent(context, MainActivity.class);
                        }
                        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                        Notification notification = new Notification(R.drawable.ic_notification, title, System.currentTimeMillis());
                        notification.setLatestEventInfo(context.getApplicationContext(), title, body, contentIntent);
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        notification.defaults |= Notification.DEFAULT_LIGHTS;
                        if (notifpublic >= 2) {
                            notification.defaults |= Notification.DEFAULT_VIBRATE;
                        }
                        if (notifpublic == 3) {
                            notification.defaults |= Notification.DEFAULT_SOUND;
                        }
                        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);
                    }
                }
            } catch (Exception e) {
                Log.e("GCMIntentService.onMessage", e.toString());
            }
        }
    }

    @Override
    protected void onError(Context context, String errorId) {
        Log.e("GCMIntentService.onError", errorId);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        Log.e("GCMIntentService.onRecoverableError", errorId);
        return super.onRecoverableError(context, errorId);
    }
}
