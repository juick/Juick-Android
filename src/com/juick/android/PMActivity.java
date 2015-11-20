/*
 * Juick
 * Copyright (C) 2008-2013, ugnich
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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.GCMIntentService;
import com.juick.R;
import java.net.URLEncoder;
import org.json.JSONObject;

/**
 *
 * @author ugnich
 */
public class PMActivity extends AppCompatActivity implements PMFragment.PMFragmentListener, View.OnClickListener {

    private static final String PMFRAGMENTID = "PMFRAGMENT";
    private String uname;
    private int uid;
    private EditText etMessage;
    private Button bSend;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ((Vibrator) context.getSystemService(Activity.VIBRATOR_SERVICE)).vibrate(250);
            String message = intent.getStringExtra("message");
            PMFragment pmf = (PMFragment) getSupportFragmentManager().findFragmentByTag(PMFRAGMENTID);
            if (message.charAt(0) == '{') {
                pmf.onNewMessages("[" + message + "]");
            } else {
                pmf.onNewMessages(message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uname = getIntent().getStringExtra("uname");
        uid = getIntent().getIntExtra("uid", 0);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(uname);

        setContentView(R.layout.pm);

        etMessage = (EditText) findViewById(R.id.editMessage);
        bSend = (Button) findViewById(R.id.buttonSend);
        bSend.setOnClickListener(this);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        PMFragment pf = new PMFragment();
        Bundle args = new Bundle();
        args.putString("uname", uname);
        args.putInt("uid", uid);
        pf.setArguments(args);
        ft.add(R.id.pmfragment, pf, PMFRAGMENTID);
        ft.commit();
    }

    public void onClick(View view) {
        if (view == bSend) {
            String msg = etMessage.getText().toString();
            if (msg.length() > 0) {
                postText(msg);
            } else {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void postText(final String body) {
        Thread thr = new Thread(new Runnable() {

            public void run() {
                try {
                    final String ret = Utils.postJSON(PMActivity.this, "https://api.juick.com/pm", "uname=" + uname + "&body=" + URLEncoder.encode(body, "utf-8"));
                    PMActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            if (ret != null) {
                                etMessage.setText("");
                                PMFragment pmf = (PMFragment) getSupportFragmentManager().findFragmentByTag(PMFRAGMENTID);
                                pmf.onNewMessages("[" + ret + "]");
                            } else {
                                Toast.makeText(PMActivity.this, R.string.Error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("postPM", e.toString());
                }
            }
        });
        thr.start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (hasFocus) {
            spe.putString("currentactivity", "pm-" + uid);
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(GCMIntentService.GCMEVENTACTION));
        } else {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
            spe.remove("currentactivity");
        }
        spe.commit();
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
