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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.juick.R;
import com.juick.android.api.JuickMessage;
import org.json.JSONArray;

/**
 *
 * @author ugnich
 */
public class PMFragment extends ListFragment {

    private PMFragmentListener parentActivity;
    private String uname;
    private int uid;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            parentActivity = (PMFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PMFragmentListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        uname = getArguments().getString("uname");
        uid = getArguments().getInt("uid", 0);

        Thread thr = new Thread(new Runnable() {

            public void run() {
                String url = "http://api.juick.com/pm?uname=" + uname;
                final String jsonStr = Utils.getJSON(getActivity(), url);
                System.out.println("PMS: " + jsonStr);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr != null) {
                                try {
                                    PMAdapter listAdapter = new PMAdapter(getActivity(), uid);
                                    listAdapter.parseJSON(jsonStr);
                                    setListAdapter(listAdapter);
                                    getListView().setSelection(listAdapter.getCount() - 1);
                                    getListView().setDividerHeight(0);
                                } catch (Exception e) {
                                    Log.e("initTagsAdapter", e.toString());
                                }
                            }
                        }
                    });
                }
            }
        });
        thr.start();
    }

    public interface PMFragmentListener {
    }
}

class PMAdapter extends ArrayAdapter<JuickMessage> {

    Context context;
    int uid;

    public PMAdapter(Context context, int uid) {
        super(context, R.layout.listitem_pm_in);
        this.context = context;
        this.uid = uid;
    }

    public int parseJSON(String jsonStr) {
        try {
            JSONArray json = new JSONArray(jsonStr);
            int cnt = json.length();
            for (int i = 0; i < cnt; i++) {
                add(JuickMessage.parseJSON(json.getJSONObject(i)));
            }
            return cnt;
        } catch (Exception e) {
            Log.e("initOpinionsAdapter", e.toString());
        }

        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        JuickMessage msg = getItem(position);

        View v = convertView;

        if (msg.User.UID == uid) {
            if (v == null || !v.getTag().toString().equals("i")) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.listitem_pm_in, null);
                v.setTag("i");
            }

            TextView tv = (TextView) v.findViewById(R.id.text);
            tv.setText(msg.Text);
        } else {
            if (v == null || !v.getTag().toString().equals("o")) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.listitem_pm_out, null);
                v.setTag("o");
            }

            TextView tv = (TextView) v.findViewById(R.id.text);
            tv.setText(msg.Text);
        }

        return v;
    }
}
