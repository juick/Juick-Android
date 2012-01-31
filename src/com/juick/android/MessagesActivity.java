/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.juick.R;

/**
 *
 * @author ugnich
 */
public class MessagesActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
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

        if (uid > 0 && uname != null) {
            setTitle("@" + uname);
        } else if (search != null) {
            setTitle(getResources().getString(R.string.Search) + ": " + search);
        } else if (tag != null) {
            String title = getResources().getString(R.string.Tag) + ": " + tag;
            if (uid == -1) {
                title += " (" + getResources().getString(R.string.Your_messages) + ")";
            }
            setTitle(title);
        } else if (place_id > 0) {
            setTitle("Location");
        } else {
            setTitle(getResources().getString(R.string.All_messages));
        }

        setContentView(R.layout.messages);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        MessagesFragment mf = new MessagesFragment();
        Bundle args = new Bundle();

        args.putInt("uid", uid);
        args.putString("uname", uname);
        args.putString("search", search);
        args.putString("tag", tag);
        args.putInt("place_id", place_id);

        mf.setArguments(args);
        ft.replace(R.id.messagesfragment, mf);
        ft.commit();
    }
}
