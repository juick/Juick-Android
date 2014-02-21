/*
 * Juick
 * Copyright (C) 2014, ugnich
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
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.juick.R;

/**
 *
 * @author ugnich
 */
public class MainActivity extends ActionBarActivity implements OnItemClickListener, WebViewClientJuick.WebViewClientListener {

    WebView wv;
    DrawerLayout dl;
    ActionBarDrawerToggle dt;
    boolean clearHistory = false;
    private ValueCallback<Uri> mUploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = "http://juick.com/";
        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                url = uri.toString();
            }
        }

        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.main);

        ListView nd = (ListView) findViewById(R.id.navigationdrawer);
        String ndtitles[] = getResources().getStringArray(R.array.NavigationDrawerTitles);
        nd.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_listitem, ndtitles));
        nd.setOnItemClickListener(this);

        dl = (DrawerLayout) findViewById(R.id.main);
        dt = new ActionBarDrawerToggle(this, dl, R.drawable.ic_drawer, R.string.DrawerOpen, R.string.DrawerClose);
        dl.setDrawerListener(dt);

        ActionBar bar = getSupportActionBar();
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);

        wv = (WebView) findViewById(R.id.webview);
        WebSettings ws = wv.getSettings();
        ws.setUserAgentString(ws.getUserAgentString() + " Juick/3.0");
        ws.setJavaScriptEnabled(true);

        wv.setWebChromeClient(new WebChromeClient() {

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                openFileChooser(mUploadMessage);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(mUploadMessage);
            }
        });

        WebViewClientJuick wvc = new WebViewClientJuick(this);
        wvc.setWebViewClientListener(this);
        wv.setWebViewClient(wvc);
        wv.loadUrl(url);

        wv.requestFocus(View.FOCUS_DOWN);
        wv.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        dt.syncState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (dt.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menuitem_refresh:
                wv.reload();
                return true;
            case R.id.menuitem_newmessage:
                clearHistory = true;
                wv.loadUrl("http://juick.com/post");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack() && !clearHistory) {
            wv.goBack();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String ndurls[] = getResources().getStringArray(R.array.NavigationDrawerURLs);
        clearHistory = true;
        wv.loadUrl(ndurls[position]);
        dl.closeDrawers();
    }

    public void setProgressBarVisible(boolean visible) {
        setSupportProgressBarIndeterminateVisibility(visible);
        if (!visible && clearHistory) {
            wv.clearHistory();
            clearHistory = false;
        }
    }
}
