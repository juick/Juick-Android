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

import com.actionbarsherlock.app.ActionBar.Tab;
import com.juick.GCMIntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gcm.GCMRegistrar;
import com.juick.R;
import java.util.List;

/**
 *
 * @author Ugnich Anton
 */
public class MainActivity extends SherlockFragmentActivity implements ActionBar.TabListener {

    public static final int ACTIVITY_SIGNIN = 2;
    public static final int ACTIVITY_PREFERENCES = 3;
    public static final int PENDINGINTENT_CONSTANT = 713242183;
    private Fragment fChats = null;
    private Fragment fMessages = null;
    private Fragment fExplore = null;
    public boolean loadingChats = false;
    public boolean loadingMessages = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null && uri.getPathSegments().size() > 0 && parseUri(uri)) {
                return;
            }
        }

        if (!Utils.hasAuth(this)) {
            startActivityForResult(new Intent(this, SignInActivity.class), ACTIVITY_SIGNIN);
            return;
        }

        try {
            GCMRegistrar.checkDevice(this);
            GCMRegistrar.checkManifest(this);
            final String regId = GCMRegistrar.getRegistrationId(this);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String prefRegId = sp.getString("gcm_regid", null);
            if (regId.length() == 0 || !regId.equals(prefRegId)) {
                GCMRegistrar.register(this, GCMIntentService.SENDER_ID);
            }
        } catch (Exception e) {
            Log.e("Juick.GCM", e.toString());
        }

        ActionBar bar = getSupportActionBar();
        bar.setHomeButtonEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        Tab tab;
        tab = bar.newTab().setTag("c").setText("Chats").setTabListener(this);
        bar.addTab(tab);
        tab = bar.newTab().setTag("f").setText("Feed").setTabListener(this);
        bar.addTab(tab);
        tab = bar.newTab().setTag("s").setText("Search").setTabListener(this);
        bar.addTab(tab);
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        String tag = tab.getTag().toString();
        if (tag.equals("c")) {
            if (fChats == null) {
                fChats = SherlockFragment.instantiate(this, ChatsFragment.class.getName());
                ft.add(android.R.id.content, fChats, "c");
            } else {
                ft.attach(fChats);
            }
        } else if (tag.equals("f")) {
            if (fMessages == null) {
                Bundle b = new Bundle();
                b.putBoolean("home", true);
                fMessages = SherlockFragment.instantiate(this, MessagesFragment.class.getName(), b);
                ft.add(android.R.id.content, fMessages, "m");
            } else {
                ft.attach(fMessages);
            }
        } else {
            if (fExplore == null) {
                fExplore = SherlockFragment.instantiate(this, ExploreFragment.class.getName());
                ft.add(android.R.id.content, fExplore, "e");
            } else {
                ft.attach(fExplore);
            }
        }
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        String tag = tab.getTag().toString();
        if (tag.equals("c")) {
            if (fChats != null) {
                ft.detach(fChats);
            }
        } else if (tag.equals("f")) {
            if (fMessages != null) {
                ft.detach(fMessages);
                fMessages = null; // ANDROID BUG
            }
        } else {
            if (fExplore != null) {
                ft.detach(fExplore);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_SIGNIN) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            } else {
                finish();
            }
        } else if (requestCode == ACTIVITY_PREFERENCES) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_preferences:
                startActivityForResult(new Intent(this, PreferencesActivity.class), ACTIVITY_PREFERENCES);
                return true;
            case R.id.menuitem_newmessage:
                startActivity(new Intent(this, NewMessageActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean parseUri(Uri uri) {
        List<String> segs = uri.getPathSegments();
        if ((segs.size() == 1 && segs.get(0).matches("\\A[0-9]+\\z"))
                || (segs.size() == 2 && segs.get(1).matches("\\A[0-9]+\\z") && !segs.get(0).equals("places"))) {
            int mid = Integer.parseInt(segs.get(segs.size() - 1));
            if (mid > 0) {
                finish();
                Intent intent = new Intent(this, ThreadActivity.class);
                intent.setData(null);
                intent.putExtra("mid", mid);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
        } else if (segs.size() == 1 && segs.get(0).matches("\\A[a-zA-Z0-9\\-]+\\z")) {
            //TODO show user
        }
        return false;
    }

    public void updateProgressWheel() {
        setSupportProgressBarIndeterminateVisibility((loadingChats || loadingMessages) ? Boolean.TRUE : Boolean.FALSE);
    }
}
