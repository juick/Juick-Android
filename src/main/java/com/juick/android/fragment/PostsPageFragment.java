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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.juick.App;
import com.juick.R;
import com.juick.android.JuickMessageMenu;
import com.juick.android.JuickMessagesAdapter;
import com.juick.android.UrlBuilder;
import com.juick.api.RestClient;
import com.juick.api.model.Post;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by gerc on 10.01.2016.
 */
public class PostsPageFragment extends BaseFragment {

    public static final String ARG_URL = "ARG_URL";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private JuickMessagesAdapter adapter;

    String apiUrl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new JuickMessagesAdapter();
        apiUrl = null;
        Bundle args = getArguments();
        if (args != null) {
            UrlBuilder url = args.getParcelable(ARG_URL);
            if(url != null)
                apiUrl = url.toString();
        }
        if(apiUrl != null)
            load(false);

        Log.d("PostsPageFragment", "load");
    }
    @Override
    public void reload(){
        super.reload();
        load(true);
    }
    private void load(final boolean isReload) {
        RestClient.getApi().getPosts(apiUrl).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.isSuccessful() && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    List<Post> posts = response.body();
                    if(posts != null) {
                        if(isReload)
                            adapter.newData(posts);
                        else
                            adapter.addData(posts);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                t.printStackTrace();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts_page, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        swipeRefreshLayout = getActivity().findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(App.getInstance(), R.color.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(() -> load(true));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);

        recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((view1, pos) -> getBaseActivity().replaceFragment(
                ThreadFragment.newInstance(adapter.getItem(pos).getMid())));
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
                RestClient.getApi().getPosts(requestUrl).enqueue(new Callback<List<Post>>() {
                    @Override
                    public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                        loading = false;
                        List<Post> posts = response.body();
                        if (posts != null)
                            adapter.addData(posts);
                    }

                    @Override
                    public void onFailure(Call<List<Post>> call, Throwable t) {
                        loading = false;
                        Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                    }
                });
                return true;
            }
        });

        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
        Log.d("PostsPageFragment", "creatview "+adapter.getItemCount());
    }

    @Override
    public void onPause() {
        super.onPause();
        swipeRefreshLayout.setRefreshing(false);
    }

}
