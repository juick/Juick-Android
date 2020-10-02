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

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.juick.App;
import com.juick.R;
import com.juick.api.model.Post;
import com.juick.api.model.SecureUser;
import com.juick.api.model.User;

import java.io.IOException;

import retrofit2.Response;

public class MessagesSyncService extends Service {

    private static class MessagesSyncAdapter extends AbstractThreadedSyncAdapter {

        private final Context context;

        public MessagesSyncAdapter(Context context) {
            super(context, true);
            this.context = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            try {
                Response<SecureUser> response = App.getInstance().getApi().me().execute();
                if (response.isSuccessful()) {
                    SecureUser me = response.body();
                    if (me.getUnreadCount() > 0) {
                        User user = new User(0, "Juick");
                        Post announcement = new Post();
                        announcement.setUser(user);
                        announcement.setBody(context.getString(R.string.unread_discussions));
                        try {
                            String messageData = App.getInstance().getJsonMapper().writeValueAsString(announcement);
                            App.getInstance().getNotificationSender().showNotification(messageData);
                        } catch (JsonProcessingException e) {
                            Log.w(this.getClass().getSimpleName(), "JSON error", e);
                        }
                    }
                }
            } catch (IOException e) {
                Log.d(this.getClass().getSimpleName(), "Sync error", e);
            }
        }
    }

    private MessagesSyncAdapter messagesSyncAdapter;

    @Override
    public void onCreate() {
        messagesSyncAdapter = new MessagesSyncAdapter(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messagesSyncAdapter.getSyncAdapterBinder();
    }
}
