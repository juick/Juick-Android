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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.juick.R;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author ugnich
 */
public class ChatsFragment extends ListFragment implements OnItemClickListener {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String jcacheMain = sp.getString("jcache_main", null);
        if (jcacheMain != null) {
            try {
                ChatsAdapter listAdapter = new ChatsAdapter(getActivity());
                listAdapter.parseJSON(jcacheMain);
                setListAdapter(listAdapter);
            } catch (Exception e) {
            }
        }

        Thread thr = new Thread(new Runnable() {

            public void run() {
                String url = "https://api.juick.com/groups_pms?cnt=10";
                final String jsonStr = Utils.getJSON(getActivity(), url);
                if (isAdded() && jsonStr != null && (jcacheMain == null || !jsonStr.equals(jcacheMain))) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            try {
                                ChatsAdapter listAdapter = (ChatsAdapter) getListAdapter();
                                if (listAdapter == null) {
                                    listAdapter = new ChatsAdapter(getActivity());
                                }
                                listAdapter.parseJSON(jsonStr);
                                setListAdapter(listAdapter);

                                SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                                spe.putString("jcache_main", jsonStr);
                                spe.commit();
                            } catch (Exception e) {
                            }
                        }
                    });
                }
            }
        });
        thr.start();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ChatsAdapter listAdapter = (ChatsAdapter) getListAdapter();
        ChatsAdapterItem item = listAdapter.getItem(position);

        Intent i = new Intent(getActivity(), PMActivity.class);
        i.putExtra("uname", item.userName);
        i.putExtra("uid", item.userID);
        startActivity(i);
    }
}

class ChatsAdapter extends ArrayAdapter<ChatsAdapterItem> {

    Context context;
    private ImageCache userpics;

    public ChatsAdapter(Context context) {
        super(context, R.layout.listitem_juickmessage);
        this.context = context;

        userpics = new ImageCache(context, "userpics-small", 1024 * 1024 * 1);
    }

    public int parseJSON(String jsonStr) {
        try {
            clear();

            JSONArray json = new JSONObject(jsonStr).getJSONArray("pms");
            int cnt = json.length();
            for (int i = 0; i < cnt; i++) {
                JSONObject j = json.getJSONObject(i);
                ChatsAdapterItem item = new ChatsAdapterItem();
                item.userName = j.getString("uname");
                item.userID = j.getInt("uid");
                if (j.has("MessagesCount")) {
                    item.unreadMessages = j.getInt("MessagesCount");
                }
                add(item);
            }
            return cnt;
        } catch (Exception e) {
            Log.e("MainAdapter.parseJSON", e.toString());
        }
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatsAdapterItem i = getItem(position);
        View v = convertView;

        if (v == null || !(v instanceof LinearLayout)) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.listitem_main, null);
        }

        TextView t = (TextView) v.findViewById(R.id.text);
        ImageView img = (ImageView) v.findViewById(R.id.icon);
        t.setText(i.userName);
        img.setVisibility(View.VISIBLE);

        Bitmap bitmap = userpics.getImageMemory(Integer.toString(i.userID));
        if (bitmap != null) {
            img.setImageBitmap(bitmap);
        } else {
            img.setImageResource(R.drawable.ic_user_32);
            ImageLoaderTask task = new ImageLoaderTask(userpics, img, true);
            task.execute(Integer.toString(i.userID), "http://i.juick.com/as/" + i.userID + ".png");
        }

        TextView unread = (TextView) v.findViewById(R.id.unreadMessages);
        if (i.unreadMessages > 0) {
            unread.setText(Integer.toString(i.unreadMessages));
            unread.setVisibility(View.VISIBLE);
        } else {
            unread.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }
}

class ChatsAdapterItem {

    String userName = null;
    int userID = 0;
    int unreadMessages = 0;
}
