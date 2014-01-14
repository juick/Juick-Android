/*
 * Juick
 * Copyright (C) 2008-2014, ugnich
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.R;

/**
 *
 * @author ugnich
 */
public class ExploreFragment extends ListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Activity parentActivity;
    private EditText etSearch;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parentActivity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.explore, container, false);

        etSearch = (EditText) v.findViewById(R.id.editSearch);
        ((Button) v.findViewById(R.id.buttonFind)).setOnClickListener(this);

        String tags[] = {"juick", "android", "сиськи"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(parentActivity, android.R.layout.simple_list_item_1, tags);
        setListAdapter(adapter);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
    }

    public void onClick(View v) {
        String search = etSearch.getText().toString();
        if (search.length() == 0) {
            Toast.makeText(parentActivity, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(parentActivity, MessagesActivity.class);
        i.putExtra("search", search);
        startActivity(i);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String tag = getListAdapter().getItem(position).toString();
        Intent i = new Intent(parentActivity, MessagesActivity.class);
        i.putExtra("tag", tag);
        startActivity(i);
    }
}
