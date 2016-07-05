package com.juick.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by vt on 21/11/15.
 */
public class MessagesFragmentPagerAdapter extends FragmentPagerAdapter {

    private String tabTitles[] = new String[] {"Home", "Discover", "Photos"};
    private String tabTags[] = new String[] {"home", "all", "media"};
    private Context context;


    public MessagesFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle b = new Bundle();
        b.putBoolean(tabTags[position], true);
        b.putBoolean("usecache", true);
        return Fragment.instantiate(context, MessagesFragment.class.getName(), b);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
