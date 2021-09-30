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
package com.juick.android;

import android.Manifest;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
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

import com.google.android.material.navigation.NavigationView;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.fragment.ChatsFragment;
import com.juick.android.fragment.DiscoverFragment;
import com.juick.android.fragment.ThreadFragment;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.GlideApp;
import com.juick.api.model.Pms;
import com.juick.api.model.Post;
import com.juick.api.model.SecureUser;
import com.juick.databinding.ActivityMainBinding;
import com.juick.util.StringUtils;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.ACTION_VIEW;

/**
 * @author Ugnich Anton
 */
public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private ActivityMainBinding model;

    private NotificationManager notificationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(model.getRoot());
        Toolbar toolbar = model.toolbar;
        setSupportActionBar(toolbar);

        DrawerLayout drawer = model.drawerLayout;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        toggle.syncState();

        final NavigationView navigationView = model.navView;
        navigationView.setNavigationItemSelectedListener(this);

        View navHeader = navigationView.getHeaderView(0).findViewById(R.id.header);
        navHeader.setOnClickListener(this);

        final ImageView imageHeader = navigationView.getHeaderView(0).findViewById(R.id.profile_image);
        if (Utils.hasAuth()) {
            App.getInstance().getApi().me().enqueue(new Callback<SecureUser>() {
                @Override
                public void onResponse(@NonNull Call<SecureUser> call, @NonNull Response<SecureUser> response) {
                    SecureUser me = response.body();
                    Utils.myId = me.getUid();
                    String avatarUrl = me.getAvatar();
                    GlideApp.with(imageHeader.getContext())
                            .load(avatarUrl)
                            .fallback(R.drawable.av_96)
                            .placeholder(R.drawable.av_96)
                            .into(imageHeader);
                }

                @Override
                public void onFailure(@NonNull Call<SecureUser> call, @NonNull Throwable t) {
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
            TextView titleHeader = navigationView.getHeaderView(0).findViewById(R.id.title_textView);
            if (!TextUtils.isEmpty(Utils.getNick())) {
                titleHeader.setText(Utils.getNick());
            }
            notificationManager = new NotificationManager(this);
            Account account = Utils.getAccount();
            if (Build.VERSION.SDK_INT >= 23
                    && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                }, ViewUtil.REQUEST_CODE_SYNC_CONTACTS);
            } else {
                ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
                ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
                ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, Bundle.EMPTY, 86400L);
            }
            JuickConfig.refresh();
        }

        navigationView.getMenu().findItem(R.id.chats).setVisible(Utils.hasAuth());
        navigationView.getMenu().findItem(R.id.feed).setVisible(Utils.hasAuth());

        if (savedInstanceState == null) {
            addFragment(new DiscoverFragment(), false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationManager != null) {
            notificationManager.onResume();
        }
        Intent intent = getIntent();
        String action = StringUtils.defaultString(intent.getAction());
        if (action.equals(BuildConfig.INTENT_NEW_EVENT_ACTION)) {
            String msg = intent.getStringExtra(getString(R.string.notification_extra));
            try {
                Post jmsg = App.getInstance().getJsonMapper().readValue(msg, Post.class);
                if (jmsg.getUser().getUid() == 0) {
                    setTitle(R.string.Discussions);
                    replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
                } else {
                    if (jmsg.getMid() == 0) {
                        replaceFragment(FeedBuilder.chatFor(jmsg.getUser().getUname(), jmsg.getUser().getUid()));
                    } else {
                        replaceFragment(ThreadFragment.newInstance(jmsg.getMid()));
                    }
                }
            } catch (IOException e) {
                Log.d(this.getClass().getSimpleName(), "Invalid JSON data", e);
            }
        }
        if (action.equals(ACTION_VIEW)) {
            String mimeType = StringUtils.defaultString(intent.getType());
            if (mimeType.equals("vnd.android.cursor.item/vnd.com.juick.profile")) {
                Uri contactUri = intent.getData();
                if (contactUri != null) {
                    ContentResolver contentResolver = getContentResolver();
                    Cursor queryResult = contentResolver.query(contactUri, null, null, null, null);
                    if (queryResult != null) {
                        queryResult.moveToFirst();
                        String name = queryResult.getString(queryResult.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
                        queryResult.close();
                        if (!TextUtils.isEmpty(name)) {
                            setTitle(name);
                            replaceFragment(FeedBuilder.feedFor(UrlBuilder.getUserPostsByName(name)));
                        }
                    }
                }
            } else {
                Uri data = intent.getData();
                if (data != null) {
                    processUri(data);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.header) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            if (Utils.hasAuth()) {
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
            ChatsFragment chatsFragment = new ChatsFragment();
            replaceFragment(chatsFragment);
            App.getInstance().getApi().groupsPms(10).enqueue(new Callback<Pms>() {
                @Override
                public void onResponse(@NonNull Call<Pms> call, @NonNull Response<Pms> response) {
                    if (response.isSuccessful()) {
                        Pms pms = response.body();
                        ((App.ChatsListener)chatsFragment).onChatsReceived(pms.getPms());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Pms> call, @NonNull Throwable t) {
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
        } else if (id == R.id.messages) {
            replaceFragment(new DiscoverFragment());
        } else if (id == R.id.feed) {
            setTitle(R.string.Discussions);
            replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
        }

        DrawerLayout drawer = model.drawerLayout;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ViewUtil.REQUEST_CODE_SYNC_CONTACTS) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContentResolver.setSyncAutomatically(Utils.getAccount(), ContactsContract.AUTHORITY, true);
                ContentResolver.addPeriodicSync(Utils.getAccount(), ContactsContract.AUTHORITY, Bundle.EMPTY, 86400L);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
