package com.juick.android;

import android.os.Bundle;

import com.juick.android.fragment.PMFragment;
import com.juick.android.fragment.PostsPageFragment;

import static com.juick.android.MainActivity.ARG_UID;
import static com.juick.android.MainActivity.ARG_UNAME;
import static com.juick.android.fragment.PostsPageFragment.ARG_URL;

public class FeedBuilder {
    public static PostsPageFragment feedFor(UrlBuilder u) {
        PostsPageFragment fragment = new PostsPageFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_URL, u);

        fragment.setArguments(args);
        return fragment;
    }
    public static PMFragment chatFor(String uname, int uid) {
        PMFragment fragment = new PMFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UNAME, uname);
        args.putInt(ARG_UID, uid);
        fragment.setArguments(args);
        return fragment;
    }
}
