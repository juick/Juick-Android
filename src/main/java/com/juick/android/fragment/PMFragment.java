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
package com.juick.android.fragment;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.juick.App;
import com.juick.R;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.model.Post;
import com.juick.databinding.FragmentPmBinding;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author ugnich
 */
public class PMFragment extends Fragment {

    private FragmentPmBinding model;

    private MessagesListAdapter<Post> adapter;

    public PMFragment() {
        super(R.layout.fragment_pm);
    }

    private String uname;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = FragmentPmBinding.bind(view);
        Bundle arguments = getArguments();

        if (arguments != null) {
            uname = PMFragmentArgs.fromBundle(getArguments()).getUname();

            adapter = new MessagesListAdapter<>(String.valueOf(Utils.getMyId()),
                    (imageView, url, object) -> Glide.with(imageView.getContext())
                            .load(url)
                            .transition(withCrossFade())
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
                    if (isAdded()) {
                        Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                    }
                }
            });
            model.input.setInputListener(input -> {
                postText(input.toString());
                ViewUtil.hideKeyboard(getActivity());
                return true;
            });
            App.getInstance().getNewMessage().observe(getViewLifecycleOwner(), (post) -> {
                onNewMessages(Collections.singletonList(post));
            });
        }
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
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}