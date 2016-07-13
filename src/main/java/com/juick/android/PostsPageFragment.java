package com.juick.android;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import com.juick.App;
import com.juick.R;
import com.juick.remote.api.RestClient;
import com.juick.remote.model.Post;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URLEncoder;
import java.util.List;

/**
 * Created by gerc on 10.01.2016.
 */
public class PostsPageFragment extends BasePageFragment {

    public static final String ARG_UID = "ARG_UID";
    public static final String ARG_UNAME = "ARG_UNAME";
    public static final String ARG_SEARCH = "ARG_SEARCH";
    public static final String ARG_PLACE_ID = "ARG_PLACE_ID";
    public static final String ARG_POPULAR = "ARG_POPULAR";
    public static final String ARG_NEW = "ARG_NEW";
    public static final String ARG_TAG = "ARG_TAG";

    private static final String ARG_HOME_TAG = "home";
    private static final String ARG_MEDIA_TAG = "media";

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    ProgressBar progressBar;
    JuickMessagesAdapter adapter;

    String apiUrl;

    public PostsPageFragment() {
    }

    public static PostsPageFragment newInstance(int uid, String uname, String search, String tag, int placeId, boolean popular) {
        PostsPageFragment fragment = new PostsPageFragment();
        Bundle args = new Bundle();

        args.putInt(PostsPageFragment.ARG_UID, uid);
        args.putString(PostsPageFragment.ARG_UNAME, uname);
        args.putString(PostsPageFragment.ARG_SEARCH, search);
        args.putString(PostsPageFragment.ARG_TAG, tag);
        args.putInt(PostsPageFragment.ARG_PLACE_ID, placeId);
        args.putBoolean(PostsPageFragment.ARG_POPULAR, popular);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int uid = 0;
        String uname = null;
        String search = null;
        String tag = null;
        boolean newPosts = false;
        int placeId = 0;
        boolean popular = false;

        Bundle args = getArguments();
        if (args != null) {
            uid = args.getInt(ARG_UID, 0);
            uname = args.getString(ARG_UNAME);
            search = args.getString(ARG_SEARCH);
            newPosts = args.getBoolean(ARG_NEW);
            tag = args.getString(ARG_TAG);
            placeId = args.getInt(ARG_PLACE_ID, 0);
            popular = args.getBoolean(ARG_POPULAR, false);
        }

        apiUrl = "/messages?1=1";
        if (uid > 0 && uname != null) {
            apiUrl += "&user_id=" + uid;
        } else if (search != null) {
            try {
                apiUrl += "&search=" + URLEncoder.encode(search, "utf-8");
            } catch (Exception e) {
                Log.e("ApiURL", e.toString());
            }
        } else if (tag != null) {
            try {
                apiUrl += "&tag=" + URLEncoder.encode(tag, "utf-8");
            } catch (Exception e) {
                Log.e("ApiURL", e.toString());
            }
            if (uid == -1) {
                apiUrl += "&user_id=-1";
            }
        } else if (placeId > 0) {
            apiUrl += "&place_id=" + placeId;
        } else if (popular) {
            apiUrl += "&popular=1";
        }
        load();

        /*if (uid > 0 && uname != null) {
            getActivity().setTitle("@" + uname);
        } else if (search != null) {
            getActivity().setTitle(getResources().getString(R.string.search) + ": " + search);
        } else if (tag != null) {
            String title = getResources().getString(R.string.tag) + ": " + tag;
            if (uid == -1) {
                title += " (" + getResources().getString(R.string.your_messages) + ")";
            }
            getActivity().setTitle(title);
        } else if (placeId > 0) {
            getActivity().setTitle("Location");
        } else if (popular) {
            getActivity().setTitle(getResources().getString(R.string.top_messages));
        } else {
            getActivity().setTitle(getResources().getString(R.string.last_messages));
        }*/

        Log.e("PostsPageFragment", "load");
    }

    private void load() {
        adapter = new JuickMessagesAdapter(false);
        RestClient.getApi().getPosts(apiUrl).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                swipeRefreshLayout.setRefreshing(false);
                recyclerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                adapter.addData(response.body());
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                t.printStackTrace();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts_page, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new JuickMessagesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int pos) {
                getBaseActivity().replaceFragment(ThreadFragment.newInstance(adapter.getItem(pos).mid));
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
                RestClient.getApi().getPosts(apiUrl + "&before_mid=" + adapter.getItem(adapter.getItemCount() - 1).mid).enqueue(new Callback<List<Post>>() {
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
                    }
                });
                return true;
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeColors(App.getColor(App.getInstance(), R.attr.colorAccent));
    //    swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                load();
            }
        });

        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
        Log.e("PostsPageFragment", "creatview "+adapter.getItemCount());
    }

    @Override
    public void onPause() {
        super.onPause();
        swipeRefreshLayout.setRefreshing(false);
    }

}
