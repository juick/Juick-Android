/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
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
package com.juick.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.juick.App;
import com.juick.R;
import com.juick.android.fragment.ChatsFragment;
import com.juick.android.fragment.PMFragment;
import com.juick.android.fragment.PostsFragment;
import com.juick.android.fragment.ThreadFragment;
import com.juick.api.RestClient;
import com.juick.api.model.User;
import com.juick.android.service.RegistrationIntentService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startService(new Intent(this, RegistrationIntentService.class));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navHeader = navigationView.getHeaderView(0).findViewById(R.id.header);
        navHeader.setOnClickListener(this);

        final ImageView imageHeader = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.profile_image);
        if (Utils.hasAuth()) {
            RestClient.getApi().getUsers(Utils.getNick()).enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    List<User> users = response.body();
                    if (users.size() > 0) {
                        Utils.myId = users.get(0).uid;
                        Glide.with(imageHeader.getContext()).load(RestClient.getImagesUrl() + "a/" + Utils.myId + ".png").into(imageHeader);
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) {
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
            TextView titleHeader = (TextView) navigationView.getHeaderView(0).findViewById(R.id.title_textView);
            if (!TextUtils.isEmpty(Utils.getNick())) {
                titleHeader.setText(Utils.getNick());
            }
        }

        navigationView.getMenu().findItem(R.id.chats).setVisible(Utils.hasAuth());
        navigationView.getMenu().findItem(R.id.newpost).setVisible(Utils.hasAuth());

        if (savedInstanceState == null) {
            addFragment(PostsFragment.newInstance(), false);
        }
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if (PUSH_ACTION.equals(action)) {
            if (intent.getBooleanExtra(PUSH_ACTION_SHOW_PM, false)) {
                replaceFragment(PMFragment.newInstance(intent.getStringExtra(ARG_UNAME), intent.getIntExtra(ARG_UID, 0)));
            } else if (intent.getBooleanExtra(PUSH_ACTION_SHOW_THREAD, false)) {
                replaceFragment(ThreadFragment.newInstance(intent.getIntExtra(ARG_MID, 0)));
            }
        }else if(Intent.ACTION_VIEW.equals(action)){
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            String threadId = (pathSegments.size() > 1) ? pathSegments.get(pathSegments.size() - 1) : pathSegments.get(0);
            try {
                replaceFragment(ThreadFragment.newInstance(Integer.parseInt(threadId)));
            }catch (NumberFormatException ex){}
        }
        //setIntent(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.header:
                showLogin();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.chats) {
            replaceFragment(ChatsFragment.newInstance());
        }else if(id == R.id.newpost){
            if (Utils.hasAuth()) {
                startActivity(new Intent(this, NewMessageActivity.class));
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
}
