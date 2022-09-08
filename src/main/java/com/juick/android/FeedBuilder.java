/*
 * Copyright (C) 2008-2022, Juick
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

package com.juick.android;

import android.os.Bundle;

import com.juick.android.fragment.PMFragment;
import com.juick.android.fragment.PostsPageFragment;

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
        args.putString(PMFragment.ARG_UNAME, uname);
        args.putInt(PMFragment.ARG_UID, uid);
        fragment.setArguments(args);
        return fragment;
    }
}
