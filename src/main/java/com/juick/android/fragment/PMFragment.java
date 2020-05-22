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
package com.juick.android.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.GlideApp;
import com.juick.api.model.Post;
import com.juick.databinding.FragmentPmBinding;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author ugnich
 */
public class PMFragment extends BaseFragment {

    public static final String ARG_UID = "ARG_UID";
    public static final String ARG_UNAME = "ARG_UNAME";

    String uname;
    int uid;

    private FragmentPmBinding model;

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            ((Vibrator) context.getSystemService(Activity.VIBRATOR_SERVICE)).vibrate(250);

            onNewMessages(new ArrayList<Post>(){{
                try {
                    add(App.getInstance().getJsonMapper().readValue(
                            intent.getStringExtra(
                                    App.getInstance().getString(R.string.notification_extra)),
                            Post.class));
                } catch (IOException e) {
                    Log.d(this.getClass().getSimpleName(), "Invalid JSON data", e);
                }
            }});
        }
    };

    private MessagesListAdapter<Post> adapter;

    public PMFragment() {
        super(R.layout.fragment_pm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = FragmentPmBinding.bind(view);

        uname = getArguments().getString(ARG_UNAME);
        uid = getArguments().getInt(ARG_UID, 0);

        getActivity().setTitle(uname);

        adapter = new MessagesListAdapter<>(String.valueOf(Utils.myId),
                (imageView, url, object) -> GlideApp.with(imageView.getContext())
                        .load(url)
                        .into(imageView));
        model.messagesList.setAdapter(adapter);

        App.getInstance().getApi().pm(uname).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(@NonNull Call<List<Post>> call, @NonNull Response<List<Post>> response) {
                if (response.isSuccessful() && isAdded()) {
                    List<Post> newPms = response.body();
                    if (newPms != null) {
                        adapter.addToEnd(newPms, false);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
        model.input.setInputListener(input -> {
            postText(input.toString());
            ViewUtil.hideKeyboard(getActivity());
            return true;
        });
    }

    public void onNewMessages(List<Post> posts) {
        Log.d("onNewMessages", posts.toString());
        if (adapter != null) {
            for (Post p : posts) {
                adapter.addToStart(p, true);
            }
        }
    }

    public void postText(final String body) {
        App.getInstance().getApi().postPm(uname, body).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(@NonNull Call<Post> call, @NonNull final Response<Post> response) {
                if (response.isSuccessful() && isAdded()) {
                    onNewMessages(new ArrayList<Post>() {{
                        add(response.body());
                    }});
                } else {
                    Toast.makeText(App.getInstance(), R.string.blacklist_error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Post> call, @NonNull Throwable t) {
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(messageReceiver, new IntentFilter(BuildConfig.INTENT_NEW_EVENT_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(messageReceiver);
    }

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}