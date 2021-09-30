/*
 * Copyright (C) 2008-2021, Juick
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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.juick.App;
import com.juick.R;
import com.juick.android.JuickMessageMenu;
import com.juick.android.JuickMessagesAdapter;
import com.juick.android.UrlBuilder;
import com.juick.api.model.Post;
import com.juick.databinding.FragmentPostsPageBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by gerc on 10.01.2016.
 */
public class PostsPageFragment extends BaseFragment {

    public static final String ARG_URL = "ARG_URL";

    private JuickMessagesAdapter adapter;

    private String apiUrl;

    private FragmentPostsPageBinding model;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new JuickMessagesAdapter();
        Bundle args = getArguments();
        if (args != null) {
            UrlBuilder url = args.getParcelable(ARG_URL);
            if(url != null)
                apiUrl = url.toString();
        }
    }
    @Override
    public void reload() {
        super.reload();
        load(true);
    }
    private void load(final boolean isReload) {
        App.getInstance().getApi().getPosts(apiUrl).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(@NonNull Call<List<Post>> call, @NonNull Response<List<Post>> response) {
                if (model != null) {
                    model.swipeContainer.setRefreshing(false);
                    model.list.setVisibility(View.VISIBLE);
                    model.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        List<Post> posts = response.body();
                        if (posts != null) {
                            if (isReload)
                                adapter.newData(posts);
                            else
                                adapter.addData(posts);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                if (model != null) {
                    model.swipeContainer.setRefreshing(false);
                    model.list.setVisibility(View.VISIBLE);
                    model.progressBar.setVisibility(View.GONE);
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public PostsPageFragment() {
        super(R.layout.fragment_posts_page);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = FragmentPostsPageBinding.bind(view);

        model.list.setAdapter(adapter);
        adapter.setOnItemClickListener((widget, pos) -> {
            if (widget.getTag() == null || !widget.getTag().equals("clicked")) {
                getBaseActivity().replaceFragment(
                        ThreadFragment.newInstance(adapter.getItem(pos).getMid()));
            }
        });
        adapter.setOnMenuListener(new JuickMessageMenu(adapter.getItems()));
        adapter.setOnLoadMoreRequestListener(new JuickMessagesAdapter.OnLoadMoreRequestListener() {
            boolean loading;
            @Override
            public boolean onLoadMore() {
                if (adapter.getItemCount() == 0) {
                    return false;
                }
                if (loading) return true;
                loading = true;
                Post lastItem = adapter.getItem(adapter.getItemCount() - 1);
                String requestUrl = apiUrl + "&before_mid=" + lastItem.getMid();
                if (apiUrl.equals(UrlBuilder.getDiscussions().toString())) {
                    requestUrl = apiUrl + "?to=" + lastItem.getTimestamp().getTime();
                }
                App.getInstance().getApi().getPosts(requestUrl).enqueue(new Callback<List<Post>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Post>> call, @NonNull Response<List<Post>> response) {
                        loading = false;
                        if (response.isSuccessful() && isAdded()) {
                            List<Post> posts = response.body();
                            if (posts != null) {
                                adapter.addData(posts);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                        loading = false;
                        if (isAdded()) {
                            Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                return true;
            }
        });

        if (adapter.getItemCount() == 0) {
            model.list.setVisibility(View.GONE);
            model.progressBar.setVisibility(View.VISIBLE);
        } else {
            model.list.setVisibility(View.VISIBLE);
            model.progressBar.setVisibility(View.GONE);
        }
        model.swipeContainer.setColorSchemeColors(ContextCompat.getColor(App.getInstance(), R.color.colorAccent));
        model.swipeContainer.setOnRefreshListener(() -> load(true));
        load(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        model.swipeContainer.setRefreshing(false);
    }

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}
