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

import android.content.Intent;
import android.os.Bundle;

import com.juick.R;
import com.juick.android.fragment.BaseFragment;
import com.juick.android.fragment.NewPostFragment;
import com.juick.databinding.ActivityNewPostBinding;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessageActivity extends BaseActivity {

    private ActivityNewPostBinding model;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ActivityNewPostBinding.inflate(getLayoutInflater());
        setContentView(model.getRoot());
        addFragment(new NewPostFragment(), false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NewPostFragment currentFragment = getCommonFragment();
        if(currentFragment != null){
            currentFragment.resetForm();
            currentFragment.handleIntent(intent);
        }
    }

    private NewPostFragment getCommonFragment() {
        BaseFragment currentFragment = this.getCurrentFragment();
        if (currentFragment.getClass().getName().equals(NewPostFragment.class.getName())) {
            return (NewPostFragment)currentFragment;
        }
        return null;
    }

    public static class BooleanReference {

        public boolean bool;

        public BooleanReference(boolean bool) {
            this.bool = bool;
        }
    }


    @Override
    public int fragmentContainerLayoutId() {
        return R.id.fragment_container;
    }

    @Override
    public int getTabsBarLayoutId() {
        return 0;
    }
}
