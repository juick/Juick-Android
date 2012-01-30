/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import com.juick.R;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author ugnich
 */
public class MainActivity extends MessagesActivity implements OnClickListener {

    public static final int MENUITEM_PREFERENCES = 1;
    public static final int ACTIVITY_SIGNIN = 2;
    public static final int ACTIVITY_PREFERENCES = 3;
    public static final int PENDINGINTENT_CONSTANT = 713242183;
    private Button bTitleWrite;
    private Button bTitleExplore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        } catch (Exception e) {
        }
        home = true;
        super.onCreate(savedInstanceState);

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

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.main_title);

        startCheckUpdates(this);

        bTitleWrite = (Button) findViewById(R.id.buttonTitleWrite);
        bTitleExplore = (Button) findViewById(R.id.buttonTitleExplore);
        bTitleWrite.setOnClickListener(this);
        bTitleExplore.setOnClickListener(this);
    }

    public static void startCheckUpdates(Context context) {
        Intent intent = new Intent(context, CheckUpdatesReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, PENDINGINTENT_CONSTANT, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int interval = Integer.parseInt(sp.getString("refresh", "5"));
        if (interval > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 5);
            am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), interval * 60000, sender);
        } else {
            am.cancel(sender);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_SIGNIN) {
            if (resultCode == RESULT_OK) {
                onCreate(Bundle.EMPTY);
            } else {
                finish();
            }
        } else if (requestCode == ACTIVITY_PREFERENCES) {
            if (!Utils.hasAuth(this)) {
                finish();
            }
        }
    }

    public void onClick(View v) {
        if (v == bTitleWrite) {
            startActivity(new Intent(this, NewMessageActivity.class));
        } else if (v == bTitleExplore) {
            startActivity(new Intent(this, ExploreActivity.class));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(MENUITEM_PREFERENCES) == null) {
            menu.add(Menu.NONE, MENUITEM_PREFERENCES, Menu.NONE, R.string.Settings).setIcon(android.R.drawable.ic_menu_preferences);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENUITEM_PREFERENCES) {
            startActivityForResult(new Intent(this, PreferencesActivity.class), ACTIVITY_PREFERENCES);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean parseUri(Uri uri) {
        List<String> segs = uri.getPathSegments();
        if ((segs.size() == 1 && segs.get(0).matches("\\A[0-9]+\\z")) ||
                (segs.size() == 2 && segs.get(1).matches("\\A[0-9]+\\z") && !segs.get(0).equals("places"))) {
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
}
