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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import com.juick.R;
import org.json.JSONArray;

/**
 *
 * @author Ugnich Anton
 */
public class TagsFragment extends ListFragment implements OnItemClickListener, OnItemLongClickListener {

    private TagsFragmentListener parentActivity;
    private ArrayAdapter<String> listAdapter;
    private View viewLoading;
    private int uid = 0;

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        try {
            parentActivity = (TagsFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TagsFragmentListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            uid = args.getInt("uid", 0);
        }

        viewLoading = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_loading, null);
        getListView().addFooterView(viewLoading, null, false);

        listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);

        Thread thr = new Thread(new Runnable() {

            public void run() {
                String url = "http://api.juick.com/tags";
                if (uid != 0) {
                    url += "?user_id=" + uid;
                }
                final String jsonStr = Utils.getJSON(getActivity(), url);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr != null) {
                                try {
                                    JSONArray json = new JSONArray(jsonStr);
                                    int cnt = json.length();
                                    for (int i = 0; i < cnt; i++) {
                                        listAdapter.add(json.getJSONObject(i).getString("tag"));
                                    }
                                } catch (Exception e) {
                                    Log.e("initTagsAdapter", e.toString());
                                }
                            }
                            TagsFragment.this.getListView().removeFooterView(viewLoading);
                        }
                    });
                }
            }
        });
        thr.start();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        parentActivity.onTagClick(listAdapter.getItem(position));
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        parentActivity.onTagLongClick(listAdapter.getItem(position));
        return true;
    }

    public interface TagsFragmentListener {

        public void onTagClick(String tag);

        public void onTagLongClick(String tag);
    }
}
