/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
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

import android.widget.AbsListView;
import com.juick.android.api.JuickMessage;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import com.juick.R;
import java.net.URLEncoder;

/**
 *
 * @author Ugnich Anton
 */
public class MessagesFragment extends ListFragment implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

    private JuickMessagesAdapter listAdapter;
    private View viewLoading;
    private String apiurl;
    private boolean loading = true;
    private int page = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean home = false;
        int uid = 0;
        String uname = null;
        String search = null;
        String tag = null;
        int place_id = 0;
        boolean popular = false;

        Bundle args = getArguments();
        if (args != null) {
            home = args.getBoolean("home", false);
            uid = args.getInt("uid", 0);
            uname = args.getString("uname");
            search = args.getString("search");
            tag = args.getString("tag");
            place_id = args.getInt("place_id", 0);
            popular = args.getBoolean("popular", false);
        }

        if (home) {
            apiurl = "http://api.juick.com/home?1=1";
        } else {
            apiurl = "http://api.juick.com/messages?1=1";
            if (uid > 0 && uname != null) {
                apiurl += "&user_id=" + uid;
            } else if (search != null) {
                try {
                    apiurl += "&search=" + URLEncoder.encode(search, "utf-8");
                } catch (Exception e) {
                    Log.e("ApiURL", e.toString());
                }
            } else if (tag != null) {
                try {
                    apiurl += "&tag=" + URLEncoder.encode(tag, "utf-8");
                } catch (Exception e) {
                    Log.e("ApiURL", e.toString());
                }
                if (uid == -1) {
                    apiurl += "&user_id=-1";
                }
            } else if (place_id > 0) {
                apiurl += "&place_id=" + place_id;
            } else if (popular) {
                apiurl += "&popular=1";
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new HeaderViewListAdapter(null, null, null));
        viewLoading = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_loading, null);
        if (getListView().getCount() != 1) {
            getListView().addFooterView(viewLoading, null, false);
        }
        listAdapter = new JuickMessagesAdapter(getActivity(), 0);
        setListAdapter(listAdapter);

        getListView().setOnScrollListener(this);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(new JuickMessageMenu(getActivity()));

        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(getActivity(), apiurl);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr == null || listAdapter.parseJSON(jsonStr) != 20) {
                                MessagesFragment.this.getListView().removeFooterView(viewLoading);
                            }
                            loading = false;
                        }
                    });
                }
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
                    final String jsonStr = Utils.getJSON(getActivity(), apiurl + "&before_mid=" + jmsg.MID + "&page=" + page);
                    if (isAdded()) {
                        getActivity().runOnUiThread(new Runnable() {

                            public void run() {
                                if (jsonStr == null || listAdapter.parseJSON(jsonStr) != 20) {
                                    MessagesFragment.this.getListView().removeFooterView(viewLoading);
                                }
                                loading = false;
                            }
                        });
                    }
                }
            });
            thr.start();
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JuickMessage jmsg = (JuickMessage) parent.getItemAtPosition(position);
        Intent i = new Intent(getActivity(), ThreadActivity.class);
        i.putExtra("mid", jmsg.MID);
        startActivity(i);
    }
}
