/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import com.juick.android.api.JuickMessage;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import com.juick.R;
import java.net.URLEncoder;

/**
 *
 * @author ugnich
 */
public class MessagesActivity extends ListActivity implements AdapterView.OnItemClickListener, OnScrollListener {

    private static final int MENUITEM_REFRESH = 2;
    private JuickMessagesAdapter listAdapter;
    private View viewLoading;
    private String apiurl;
    private boolean loading = true;
    private int page = 1;
    boolean home = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
//        boolean home = i.getBooleanExtra("home", false);
        int uid = i.getIntExtra("uid", 0);
        String uname = i.getStringExtra("uname");
        String search = i.getStringExtra("search");
        String tag = i.getStringExtra("tag");
        int place_id = i.getIntExtra("place_id", 0);

        if (i.getData() != null) {
            Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                uid = cursor.getInt(cursor.getColumnIndex("DATA1"));
                uname = cursor.getString(cursor.getColumnIndex("DATA2"));
            }
        }

        if (home) {
            apiurl = "http://api.juick.com/home?1=1";
        } else {
            apiurl = "http://api.juick.com/messages?1=1";
            if (uid > 0 && uname != null) {
                apiurl += "&user_id=" + uid;
                setTitle("@" + uname);
            } else if (search != null) {
                try {
                    apiurl += "&search=" + URLEncoder.encode(search, "utf-8");
                } catch (Exception e) {
                    Log.e("ApiURL", e.toString());
                }
                setTitle(getResources().getString(R.string.Search) + ": " + search);
            } else if (tag != null) {
                try {
                    apiurl += "&tag=" + URLEncoder.encode(tag, "utf-8");
                } catch (Exception e) {
                    Log.e("ApiURL", e.toString());
                }
                String title = getResources().getString(R.string.Tag) + ": " + tag;
                if (uid == -1) {
                    apiurl += "&user_id=-1";
                    title += " (" + getResources().getString(R.string.Your_messages) + ")";
                }

                setTitle(title);
            } else if (place_id > 0) {
                apiurl += "&place_id=" + place_id;
                setTitle("Location");
            } else {
                setTitle(getResources().getString(R.string.All_messages));
            }
        }

        getListView().setOnScrollListener(this);

        initAdapter();
    }

    private void initAdapter() {
        getListView().setAdapter(new HeaderViewListAdapter(null, null, null));

        viewLoading = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_loading, null);
        if (getListView().getCount() != 1) {
            getListView().addFooterView(viewLoading, null, false);
        }

        listAdapter = new JuickMessagesAdapter(this, 0);
        getListView().setAdapter(listAdapter);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(new JuickMessageMenu(this));

        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(MessagesActivity.this, apiurl);
                MessagesActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        if (jsonStr == null || listAdapter.parseJSON(jsonStr) != 20) {
                            MessagesActivity.this.getListView().removeFooterView(viewLoading);
                        }
                        loading = false;
                    }
                });
            }
        });
        thr.start();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (visibleItemCount < totalItemCount && (firstVisibleItem + visibleItemCount == totalItemCount) && loading == false) {
            loading = true;
            page++;
            final JuickMessage jmsg = (JuickMessage) listAdapter.getItem(listAdapter.getCount() - 1);

            Thread thr = new Thread(new Runnable() {

                public void run() {
                    final String jsonStr = Utils.getJSON(MessagesActivity.this, apiurl + "&before_mid=" + jmsg.MID + "&page=" + page);
                    MessagesActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr == null || listAdapter.parseJSON(jsonStr) != 20) {
                                MessagesActivity.this.getListView().removeFooterView(viewLoading);
                            }
                            loading = false;
                        }
                    });
                }
            });
            thr.start();
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JuickMessage jmsg = (JuickMessage) parent.getItemAtPosition(position);
        Intent i = new Intent(this, ThreadActivity.class);
        i.putExtra("mid", jmsg.MID);
        startActivity(i);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(MENUITEM_REFRESH) == null) {
            menu.add(Menu.NONE, MENUITEM_REFRESH, Menu.NONE, R.string.Refresh).setIcon(android.R.drawable.ic_menu_rotate);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENUITEM_REFRESH) {
            initAdapter();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
