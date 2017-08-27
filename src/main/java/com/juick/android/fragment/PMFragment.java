/*
 * Juick
 * Copyright (C) 2008-2013, ugnich
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
package com.juick.android.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.bluelinelabs.logansquare.LoganSquare;
import com.juick.App;
import com.juick.R;
import com.juick.android.PMAdapter;
import com.juick.api.RestClient;
import com.juick.api.model.Post;
import com.juick.android.service.GCMReceiverService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ugnich
 */
public class PMFragment extends BaseFragment implements View.OnClickListener {

    private static final String ARG_UID = "ARG_UID";
    private static final String ARG_UNAME = "ARG_UNAME";

    String uname;
    int uid;
    EditText etMessage;
    ImageView bSend;
    RecyclerView recyclerView;

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            ((Vibrator) context.getSystemService(Activity.VIBRATOR_SERVICE)).vibrate(250);

            onNewMessages(new ArrayList<Post>(){{
                try {
                    add(LoganSquare.parse(intent.getStringExtra("message"), Post.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }});
        }
    };

    private PMAdapter adapter;

    public PMFragment() {
    }

    public static PMFragment newInstance(String uname, int uid) {
        PMFragment fragment = new PMFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UNAME, uname);
        args.putInt(ARG_UID, uid);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pm, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        uname = getArguments().getString(ARG_UNAME);
        uid = getArguments().getInt(ARG_UID, 0);

        getActivity().setTitle(uname);

        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        adapter = new PMAdapter(uid);
        recyclerView.setAdapter(adapter);

        RestClient.getApi().pm(uname).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    adapter.addData(response.body());
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });

        etMessage = (EditText) view.findViewById(R.id.editMessage);
        bSend = (ImageView) view.findViewById(R.id.buttonSend);
        bSend.setOnClickListener(this);
    }

    public void onNewMessages(List<Post> posts) {
        Log.d("onNewMessages", posts.toString());
        if (adapter != null && posts != null) {
            adapter.addData(posts);
        }
    }

    public void onClick(View view) {
        if (view == bSend) {
            String msg = etMessage.getText().toString();
            if (msg.length() > 0) {
                postText(msg);
            } else {
                Toast.makeText(App.getInstance(), R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void postText(final String body) {
        RestClient.getApi().postPm(uname, body).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, final Response<Post> response) {
                etMessage.setText("");
                onNewMessages(new ArrayList<Post>() {{
                    add(response.body());
                }});
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
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
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(messageReceiver, new IntentFilter(GCMReceiverService.GCM_EVENT_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(messageReceiver);
    }
}