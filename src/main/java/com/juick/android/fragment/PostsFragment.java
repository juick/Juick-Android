package com.juick.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.juick.App;
import com.juick.R;
import com.juick.android.NewMessageActivity;
import com.juick.android.Utils;
import com.juick.android.widget.ScrollingFABBehavior;

/**
 * Created by gerc on 03.06.2016.
 */
public class PostsFragment extends BaseTabsFragment implements View.OnClickListener {

    public PostsFragment() {
    }

    public static PostsFragment newInstance() {
        PostsFragment fragment = new PostsFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts_viewpager, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) getBaseActivity().findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        FloatingActionButton fab = (FloatingActionButton) getBaseActivity().findViewById(R.id.fab);

        fab.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFABEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        setFABEnabled(false);
    }

    private void setFABEnabled(boolean enabled) {
        FloatingActionButton fab = (FloatingActionButton) getBaseActivity().findViewById(R.id.fab);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        ((ScrollingFABBehavior) lp.getBehavior()).setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                if (Utils.hasAuth()) {
                    startActivity(new Intent(getContext(), NewMessageActivity.class));
                } else {
                    getBaseActivity().showLogin();
                }
                break;
        }
    }

    public static class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private String tabTitles[] = new String[] {App.getInstance().getString(R.string.Top_messages), App.getInstance().getString(R.string.Last_messages)};

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PostsPageFragment.newInstance(0, null, null, null, 0, position == 0);
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }
    }
}
