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

package com.juick.android.service;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.Utils;

import java.util.Map;

/**
 * Created by vt on 03/12/15.
 */
public class FirebaseReceiverService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> data = message.getData();
        String msg = data.get(App.getInstance().getString(R.string.notification_extra));
        Log.d(FirebaseReceiverService.class.getSimpleName(), "onMessageReceived " + data.toString());
        boolean isForegroundMessage = message.getNotification() != null;
        if (isForegroundMessage) {
            Log.d(FirebaseReceiverService.class.getSimpleName(), "Message received in foreground");
            LocalBroadcastManager.getInstance(App.getInstance())
                    .sendBroadcast(new Intent(BuildConfig.INTENT_NEW_EVENT_ACTION)
                            .putExtra(getString(R.string.notification_extra), msg));
        } else {
            App.getInstance().getNotificationSender().showNotification(msg);
        }
    }
    @Override
    public void onNewToken(@NonNull String refreshedToken) {
        Log.d(FirebaseReceiverService.class.getSimpleName(), "Refreshed token: " + refreshedToken);
        Utils.updateFCMToken(refreshedToken);
    }
}
