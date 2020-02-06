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
package com.juick.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;
import com.juick.App;
import com.juick.R;
import com.juick.android.fragment.ChatsFragment;
import com.juick.android.fragment.DiscoverFragment;
import com.juick.android.fragment.ThreadFragment;
import com.juick.android.service.MessageChecker;
import com.juick.api.GlideApp;
import com.juick.api.RestClient;
import com.juick.api.model.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    public static final String ARG_UID = "ARG_UID";
    public static final String ARG_MID = "ARG_MID";
    public static final String ARG_UNAME = "ARG_UNAME";
    public static final String PUSH_ACTION = "PUSH_ACTION";
    public static final String PUSH_ACTION_SHOW_THREAD = "PUSH_ACTION_SHOW_THREAD";
    public static final String PUSH_ACTION_SHOW_PM = "PUSH_ACTION_SHOW_PM";
    public static final String PUSH_ACTION_SHOW_DISCUSSIONS = "PUSH_ACTION_SHOW_DISCUSSIONS";

    private OkHttpClient es;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navHeader = navigationView.getHeaderView(0).findViewById(R.id.header);
        navHeader.setOnClickListener(this);

        final ImageView imageHeader = navigationView.getHeaderView(0).findViewById(R.id.profile_image);
        if (Utils.hasAuth()) {
            RestClient.getApi().getUsers(Utils.getNick()).enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    List<User> users = response.body();
                    if (users != null && users.size() > 0) {
                        Utils.myId = users.get(0).getUid();
                        String avatarUrl = users.get(0).getAvatar();
                        GlideApp.with(imageHeader.getContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.av_96)
                                .into(imageHeader);
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) {
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
            TextView titleHeader = navigationView.getHeaderView(0).findViewById(R.id.title_textView);
            if (!TextUtils.isEmpty(Utils.getNick())) {
                titleHeader.setText(Utils.getNick());
            }
            if (GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    String token = instanceIdResult.getToken();
                    Utils.updateFCMToken(token);
                });
            } else {
                PeriodicWorkRequest periodicWorkRequest =
                        new PeriodicWorkRequest.Builder(MessageChecker.class, 1, TimeUnit.HOURS)
                                .build();
                WorkManager workManager = WorkManager.getInstance();
                workManager.enqueueUniquePeriodicWork(this.getClass().getSimpleName(),
                        ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
            }
        }

        navigationView.getMenu().findItem(R.id.chats).setVisible(Utils.hasAuth());
        navigationView.getMenu().findItem(R.id.feed).setVisible(Utils.hasAuth());

        if (savedInstanceState == null) {
            addFragment(new DiscoverFragment(), false);
        }
        onNewIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        != ConnectionResult.SUCCESS) {
            Log.d(this.getClass().getSimpleName(), "Play Services unavailable, using direct connection");
            initEventsListener();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if (PUSH_ACTION.equals(action)) {
            if (intent.getBooleanExtra(PUSH_ACTION_SHOW_PM, false)) {
                replaceFragment(FeedBuilder.chatFor(intent.getStringExtra(ARG_UNAME), intent.getIntExtra(ARG_UID, 0)));
            } else if (intent.getBooleanExtra(PUSH_ACTION_SHOW_THREAD, false)) {
                replaceFragment(ThreadFragment.newInstance(intent.getIntExtra(ARG_MID, 0)));
            } else if (intent.getBooleanExtra(PUSH_ACTION_SHOW_DISCUSSIONS, false)) {
                setTitle(R.string.Discussions);
                replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
            }
        }else if(Intent.ACTION_VIEW.equals(action)){
            Uri data = intent.getData();
            processUri(data);

        }
        //setIntent(null);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.header) {
            if (Utils.hasAuth()) {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                setTitle(R.string.Subscriptions);
                replaceFragment(FeedBuilder.feedFor(UrlBuilder.goHome()));
            } else {
                showLogin();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.chats) {
            replaceFragment(ChatsFragment.newInstance());
        } else if(id == R.id.messages) {
            replaceFragment(new DiscoverFragment());
        } else if (id == R.id.feed) {
            setTitle(R.string.Discussions);
            replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public int fragmentContainerLayoutId() {
        return R.id.fragment_container;
    }

    @Override
    public int getTabsBarLayoutId() {
        return R.id.tabs;
    }

    public void processUri(Uri data) {
        List<String> pathSegments = data.getPathSegments();
        switch (pathSegments.size()) {
            case 1:
                // blog
                replaceFragment(FeedBuilder.feedFor(UrlBuilder.getUserPostsByName(pathSegments.get(0))));
                break;
            case 2:
                // thread
                String threadId = pathSegments.get(1);
                try {
                    replaceFragment(ThreadFragment.newInstance(Integer.parseInt(threadId)));
                } catch (NumberFormatException ex) {
                }
                break;
            default:
                // discover
                replaceFragment(new DiscoverFragment());
                break;
        }
    }
    private void initEventsListener() {
        if (es != null) return;
        es = Utils.getSSEFactory()
                .readTimeout(0, TimeUnit.SECONDS).build();
        Request request = new Request.Builder()
                .url("https://juick.com/api/events")
                .build();
        OkSse sse = new OkSse(es);
        sse.newServerSentEvent(request, new ServerSentEvent.Listener() {
            @Override
            public void onOpen(ServerSentEvent sse, okhttp3.Response response) {
                Log.d("SSE", "Event listener opened");
            }

            @Override
            public void onMessage(ServerSentEvent sse, String id, String event, String message) {
                Log.d("SSE", "event received: " + event);
                if (event.equals("msg")) {
                    LocalBroadcastManager.getInstance(App.getInstance())
                            .sendBroadcast(new Intent(RestClient.ACTION_NEW_EVENT)
                                    .putExtra(RestClient.NEW_EVENT_EXTRA, message));
                }
            }

            @Override
            public void onComment(ServerSentEvent sse, String comment) {

            }

            @Override
            public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
                return true;
            }

            @Override
            public boolean onRetryError(ServerSentEvent sse, Throwable throwable, okhttp3.Response response) {
                return true;
            }

            @Override
            public void onClosed(ServerSentEvent sse) {
            }

            @Override
            public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
                return originalRequest;
            }
        });
    }
}
