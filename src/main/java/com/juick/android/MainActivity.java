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
package com.juick.android;

import static android.content.Intent.ACTION_VIEW;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.screens.chats.ChatsFragmentDirections;
import com.juick.android.screens.home.HomeFragmentDirections;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.model.Post;
import com.juick.databinding.ActivityMainBinding;
import com.juick.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Ugnich Anton
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding model;

    private NotificationManager notificationManager;

    private ActivityResultLauncher<Intent> loginLauncher;

    public void showLogin() {
        if (!Utils.hasAuth()) {
            loginLauncher.launch(new Intent(this, SignInActivity.class));
        }
    }

    AppBarConfiguration appBarConfiguration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(model.getRoot());
        Toolbar toolbar = model.toolbar;
        //toolbar.inflateMenu(R.menu.toolbar);
        setSupportActionBar(toolbar);
        //CollapsingToolbarLayout layout = model.collapsingToolbarLayout;
        BottomNavigationView navView = model.bottomNav;
        FloatingActionButton fab = model.fab;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.home,
                R.id.discover,
                R.id.chats,
                R.id.no_auth,
                R.id.profile,
                R.id.new_post
        )
        .build();
        NavHostFragment navHostFragment = model.navHost.getFragment();
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        //NavigationUI.setupWithNavController(toolbar, navController);
        NavigationUI.setupWithNavController(navView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            int scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS;
            if (shouldHideNavView(id)) {
                navView.setVisibility(View.GONE);
                scrollFlags = 0;
            } else {
                navView.setVisibility(View.VISIBLE);
            }
            ((AppBarLayout.LayoutParams) toolbar.getLayoutParams())
                    .setScrollFlags(scrollFlags);
            int fabVisibility = shouldViewFab(id) ? View.VISIBLE : View.GONE;
            fab.setVisibility(fabVisibility);
        });
        if (Utils.hasAuth()) {
            notificationManager = new NotificationManager();
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

        App.getInstance().setAuthorizationCallback(() -> {
            Intent updatePasswordIntent = new Intent(this, SignInActivity.class);
            updatePasswordIntent.putExtra(SignInActivity.EXTRA_ACTION, SignInActivity.ACTION_PASSWORD_UPDATE);
            loginLauncher.launch(updatePasswordIntent);
        });
        loginLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), response -> {
            if (response.getResultCode() == RESULT_OK) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
        App.getInstance().getSignInStatus().observe(this, signInStatus -> {
            if (signInStatus == SignInActivity.SignInStatus.SIGN_IN_PROGRESS) {
                showLogin();
            }
        });
        model.fab.setOnClickListener(fabView -> {
            if (Utils.hasAuth()) {
                navController.navigate(R.id.new_post);
            } else {
                showLogin();
            }
        });
    }

    boolean shouldHideNavView(int view) {
        return view == R.id.thread || view == R.id.PMFragment
                || view == R.id.new_post || view == R.id.tags;
    }

    boolean shouldViewFab(int view) {
        return view == R.id.home || view == R.id.discover;
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
                    //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
                } else {
                    if (jmsg.getMid() == 0) {
                        ChatsFragmentDirections.ActionChatsToPMFragment chatAction =
                                ChatsFragmentDirections.actionChatsToPMFragment(jmsg.getUser().getUname());
                        chatAction.setUid(jmsg.getUser().getUid());
                        NavHostFragment navHostFragment = (NavHostFragment) model.navHost.getFragment();
                        NavController navController = navHostFragment.getNavController();
                        navController.navigate(chatAction);
                    } else {
                        HomeFragmentDirections.ActionDiscoverFragmentToThreadFragment discoverAction =
                                HomeFragmentDirections.actionDiscoverFragmentToThreadFragment();
                        discoverAction.setMid(jmsg.getMid());
                        NavHostFragment navHostFragment = (NavHostFragment) model.navHost.getFragment();
                        NavController navController = navHostFragment.getNavController();
                        navController.navigate(discoverAction);
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
                            //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getUserPostsByName(name)));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void processUri(Uri data) {
        List<String> pathSegments = data.getPathSegments();
        NavHostFragment navHostFragment = (NavHostFragment) model.navHost.getFragment();
        NavController navController = navHostFragment.getNavController();
        switch (pathSegments.size()) {
            case 1:
                // blog
                //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getUserPostsByName(pathSegments.get(0))));
                break;
            case 2:
                // thread
                String threadId = pathSegments.get(1);
                HomeFragmentDirections.ActionDiscoverFragmentToThreadFragment action
                        = HomeFragmentDirections.actionDiscoverFragmentToThreadFragment();
                action.setMid(Integer.parseInt(threadId));
                navController.navigate(action);
                break;
            default:
                // discover
                navController.navigate(R.id.home);
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = model.navHost.getFragment();
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NavHostFragment navHostFragment = model.navHost.getFragment();
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
