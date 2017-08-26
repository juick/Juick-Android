package com.juick.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.juick.App;
import com.juick.R;
import com.juick.android.NewMessageActivity;
import com.juick.android.UrlBuilder;
import com.juick.android.Utils;

/**
 * Created by gerc on 03.06.2016.
 */
public class PostsFragment extends BaseTabsFragment implements View.OnClickListener {

    AppBarLayout.OnOffsetChangedListener offsetChangedListener;

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
        getActivity().setTitle(R.string.Juick);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) getBaseActivity().findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        final FloatingActionButton fab = (FloatingActionButton) getBaseActivity().findViewById(R.id.fab);
        fab.setOnClickListener(this);
        fab.show();

        AppBarLayout appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.app_bar_layout);
        offsetChangedListener = new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) >= (appBarLayout.getTotalScrollRange())) {
                    fab.hide();
                } else {
                    fab.show();
                }
            }
        };
        appBarLayout.addOnOffsetChangedListener(offsetChangedListener);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams())
                .setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FloatingActionButton fab = (FloatingActionButton) getBaseActivity().findViewById(R.id.fab);
        fab.hide();
        AppBarLayout appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.app_bar_layout);
        appBarLayout.removeOnOffsetChangedListener(offsetChangedListener);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(0);
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

        private String tabTitles[] = new String[] {
                App.getInstance().getString(R.string.Subscriptions),
                App.getInstance().getString(R.string.Last_messages),
                App.getInstance().getString(R.string.With_photos),
                App.getInstance().getString(R.string.Top_messages),
                App.getInstance().getString(R.string.Blog_messages)
        };

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }



        @Override
        public Fragment getItem(int position) {
            UrlBuilder u = null;
            switch (position){
                case 0:
                    if(Utils.hasAuth())
                        u = UrlBuilder.goHome();
                    else
                        return new NoAuthFragment();
                    break;
                case 1:
                    u = UrlBuilder.getLast();
                    break;
                case 2:
                    u = UrlBuilder.getPhotos();
                    break;
                case 3:
                    u = UrlBuilder.getTop();
                    break;
                case 4:
                default:
                    if(Utils.hasAuth())
                        u = UrlBuilder.getUserPostsByName(Utils.getNick());
                    else
                        return new NoAuthFragment();
                    break;
            }
            return PostsPageFragment.newInstance(u);
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
