/*
 * Copyright (C) 2008-2020, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.juick.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.juick.App;
import com.juick.R;
import com.juick.android.FeedBuilder;
import com.juick.android.NewMessageActivity;
import com.juick.android.UrlBuilder;
import com.juick.android.Utils;

/**
 * Created by gerc on 03.06.2016.
 */
public class DiscoverFragment extends BaseFragment implements View.OnClickListener {

    private AppBarLayout.OnOffsetChangedListener offsetChangedListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts_viewpager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.Juick);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        ViewPager viewPager = view.findViewById(R.id.viewpager);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = getBaseActivity().findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setVisibility(View.VISIBLE);

        final FloatingActionButton fab = getBaseActivity().findViewById(R.id.fab);
        fab.setOnClickListener(this);
        fab.show();

        AppBarLayout appBarLayout = getActivity().findViewById(R.id.app_bar_layout);
        offsetChangedListener = (appBarLayout1, verticalOffset) -> {
            if (Math.abs(verticalOffset) >= (appBarLayout1.getTotalScrollRange())) {
                fab.hide();
            } else {
                fab.show();
            }
        };
        appBarLayout.addOnOffsetChangedListener(offsetChangedListener);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams())
                .setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                        | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FloatingActionButton fab = getBaseActivity().findViewById(R.id.fab);
        fab.hide();
        AppBarLayout appBarLayout = getActivity().findViewById(R.id.app_bar_layout);
        appBarLayout.removeOnOffsetChangedListener(offsetChangedListener);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(0);
        appBarLayout.requestLayout();
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
                App.getInstance().getString(R.string.Last_messages),
                App.getInstance().getString(R.string.With_photos),
                App.getInstance().getString(R.string.Top_messages)
        };

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            UrlBuilder u;
            switch (position){
                case 0:
                    u = UrlBuilder.getLast();
                    break;
                case 1:
                    u = UrlBuilder.getPhotos();
                    break;
                case 2:
                    u = UrlBuilder.getTop();
                    break;
                default:
                    if(Utils.hasAuth())
                        u = UrlBuilder.getUserPostsByName(Utils.getNick());
                    else
                        return new NoAuthFragment();
                    break;
            }
            return FeedBuilder.feedFor(u);
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
