/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.juick.R;

/**
 *
 * @author ugnich
 */
public class ExploreActivity extends ListActivity implements View.OnClickListener, OnItemClickListener {

    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.explore);

        getListView().setAdapter(new ExploreAdapter(this));
        getListView().setOnItemClickListener(this);

        etSearch = (EditText) findViewById(R.id.editSearch);
        ((Button) findViewById(R.id.buttonFind)).setOnClickListener(this);
    }

    public void onClick(View v) {
        String search = etSearch.getText().toString();
        if (search.length() == 0) {
            Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, MessagesActivity.class);
        i.putExtra("search", search);
        startActivity(i);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, TagsActivity.class));
                break;
            case 1:
                startActivity(new Intent(this, MessagesActivity.class));
                break;
            case 2:
                startActivity(new Intent(this, PlacesActivity.class));
                break;
        }
    }

    class ExploreAdapter extends BaseAdapter {

        private Context context;
        private final int icons[] = {
            R.drawable.ic_list_tags,
            R.drawable.ic_list_messages,
            R.drawable.ic_list_places
        };
        private final int labels[] = {
            R.string.Popular_tags,
            R.string.All_messages,
            R.string.Nearby_places
        };

        public ExploreAdapter(Context context) {
            super();
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = (TextView) convertView;
            if (tv == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                tv = (TextView) vi.inflate(android.R.layout.simple_list_item_1, null);
                tv.setCompoundDrawablePadding(10);
            }
            if (position >= 0 && position < 4) {
                tv.setText(labels[position]);
                tv.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0);
            }
            return tv;
        }

        public int getCount() {
            return labels.length;
        }

        public long getItemId(int position) {
            return position;
        }

        public Object getItem(int position) {
            if (position >= 0 && position < 4) {
                return context.getResources().getString(labels[position]);
            } else {
                return "";
            }
        }
    }
}
