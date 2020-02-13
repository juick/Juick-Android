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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.juick.R;
import com.juick.api.RestClient;
import com.juick.api.model.Post;
import com.juick.api.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageChecker extends Worker {

    private Context context;

    public MessageChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        RestClient.getInstance().getApi().me().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    User me = response.body();
                    if (me != null && me.getUnreadCount() > 0) {
                        User user = new User(0, "Juick");
                        Post announcement = new Post();
                        announcement.setUser(user);
                        announcement.setBody(context.getString(R.string.unread_discussions));
                        try {
                            String messageData = RestClient.getJsonMapper().writeValueAsString(announcement);
                            FCMReceiverService.showNotification(messageData);
                        } catch (JsonProcessingException e) {
                            Log.w(this.getClass().getSimpleName(), "JSON error", e);
                        }

                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.w(this.getClass().getSimpleName(), "Network error", t);
            }
        });
        return Result.retry();
    }
}
