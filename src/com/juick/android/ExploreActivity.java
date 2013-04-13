/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
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
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.R;

/**
 *
 * @author Ugnich Anton
 */
public class ExploreActivity extends FragmentActivity implements View.OnClickListener, TagsFragment.TagsFragmentListener {

    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.explore);

        etSearch = (EditText) findViewById(R.id.editSearch);
        ((Button) findViewById(R.id.buttonFind)).setOnClickListener(this);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        TagsFragment tf = new TagsFragment();
        ft.add(R.id.tagsfragment, tf);
        ft.commit();
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

    public void onTagClick(String tag) {
        Intent i = new Intent(this, MessagesActivity.class);
        i.putExtra("tag", tag);
        startActivity(i);
    }

    public void onTagLongClick(String tag) {
        onTagClick(tag);
    }
}
