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

package com.juick.android.fragment;

import androidx.fragment.app.Fragment;

import com.juick.android.BaseActivity;

/**
 * Created by gerc on 10.01.2016.
 */
public class BaseFragment extends Fragment {

    public BaseFragment() {

    }

    public BaseFragment(int layoutId) {
        super(layoutId);
    }

    BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    public void reload(){

    }
}
