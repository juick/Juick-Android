/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.juick.R;

/**
 *
 * @author ugnich
 */
public class TagsActivity extends FragmentActivity implements TagsFragment.TagsFragmentListener {

    private int uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uid = getIntent().getIntExtra("uid", 0);

        if (uid == 0) {
            setTitle(R.string.Popular_tags);
        }

        setContentView(R.layout.tags);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        TagsFragment tf = new TagsFragment();
        Bundle args = new Bundle();
        args.putInt("uid", uid);
        tf.setArguments(args);
        ft.add(R.id.tagsfragment, tf);
        ft.commit();
    }

    public void onTagClick(String tag) {
        Intent i = new Intent();
        i.putExtra("tag", tag);
        setResult(RESULT_OK, i);
        finish();
    }

    public void onTagLongClick(String tag) {
        Intent i = new Intent(this, MessagesActivity.class);
        i.putExtra("tag", tag);
        i.putExtra("uid", uid);
        startActivity(i);
    }
}
