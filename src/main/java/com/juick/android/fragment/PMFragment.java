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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.juick.api.RestClient;
import com.juick.api.model.Post;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
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

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            ((Vibrator) context.getSystemService(Activity.VIBRATOR_SERVICE)).vibrate(250);

            onNewMessages(new ArrayList<Post>(){{
                try {
                    add(RestClient.getJsonMapper().readValue(intent.getStringExtra("message"), Post.class));
                } catch (IOException e) {
                    Log.d(this.getClass().getSimpleName(), "Invalid JSON data", e);
                }
            }});
        }
    };

    private MessagesListAdapter<Post> adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        uname = getArguments().getString(ARG_UNAME);
        uid = getArguments().getInt(ARG_UID, 0);

        getActivity().setTitle(uname);

        adapter = new MessagesListAdapter<>(String.valueOf(Utils.myId),
                (imageView, url, object) -> GlideApp.with(imageView.getContext())
                        .load(url)
                        .into(imageView));
        MessagesList messagesList = getActivity().findViewById(R.id.messagesList);
        messagesList.setAdapter(adapter);

        RestClient.getInstance().getApi().pm(uname).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.isSuccessful() && isAdded()) {
                    adapter.addToEnd(response.body(), false);
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
        MessageInput messageInput = getActivity().findViewById(R.id.input);
        messageInput.setInputListener(input -> {
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
        RestClient.getInstance().getApi().postPm(uname, body).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, final Response<Post> response) {
                if (response.isSuccessful() && isAdded()) {
                    onNewMessages(new ArrayList<Post>() {{
                        add(response.body());
                    }});
                } else {
                    Toast.makeText(App.getInstance(), R.string.blacklist_error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {
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
}