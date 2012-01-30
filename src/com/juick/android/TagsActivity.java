/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
 * @author ugnich
 */
public class TagsActivity extends ListActivity implements OnItemClickListener, OnItemLongClickListener {

    private ArrayAdapter<String> listAdapter;
    private View viewLoading;
    private String action;
    private int uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        action = getIntent().getAction();
        if (action == null || !action.equals(Intent.ACTION_PICK)) {
            action = Intent.ACTION_VIEW;
        }
        uid = getIntent().getIntExtra("uid", 0);

        if (uid == 0) {
            setTitle(R.string.Popular_tags);
        }

        viewLoading = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_loading, null);
        getListView().addFooterView(viewLoading, null, false);

        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        getListView().setAdapter(listAdapter);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);

        Thread thr = new Thread(new Runnable() {

            public void run() {
                String url = "http://api.juick.com/tags";
                if (uid != 0) {
                    url += "?user_id=" + uid;
                }
                final String jsonStr = Utils.getJSON(TagsActivity.this, url);
                TagsActivity.this.runOnUiThread(new Runnable() {

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
                        TagsActivity.this.getListView().removeFooterView(viewLoading);
                    }
                });
            }
        });
        thr.start();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (action.equals(Intent.ACTION_VIEW)) {
            Intent i = new Intent(this, MessagesActivity.class);
            i.putExtra("tag", listAdapter.getItem(position));
            i.putExtra("uid", uid);
            startActivity(i);
        } else if (action.equals(Intent.ACTION_PICK)) {
            Intent i = new Intent();
            i.putExtra("tag", listAdapter.getItem(position));
            setResult(RESULT_OK, i);
            finish();
        }
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Intent i = new Intent(this, MessagesActivity.class);
        i.putExtra("tag", listAdapter.getItem(position));
        i.putExtra("uid", uid);
        startActivity(i);
        return true;
    }
}
