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

package com.juick.android.screens.menu;

import android.app.Activity;
import android.content.Context;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.bumptech.glide.Glide;
import com.juick.R;
import com.juick.android.screens.me.MeViewModel;
import com.juick.databinding.ProfileMenuLayoutBinding;

public class ProfileProvider extends ActionProvider {

    ProfileMenuLayoutBinding binding;

    public ProfileProvider(@NonNull Context context) {
        super(context);
        binding = ProfileMenuLayoutBinding.inflate(LayoutInflater.from(context));
        MeViewModel meVM = new ViewModelProvider((ViewModelStoreOwner) context).get(MeViewModel.class);
        meVM.getMe().observe((LifecycleOwner) context, user -> {
            if (user.getUid() > 0) {
                ImageView profileImage = binding.profileImage;
                String avatarUrl = user.getAvatar();
                Glide.with(context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.av_96)
                        .into(profileImage);
            }
        });
    }

    @NonNull
    @Override
    public View onCreateActionView() {
        return binding.getRoot();
    }

    @NonNull
    @Override
    public View onCreateActionView(@NonNull MenuItem forItem) {
        return binding.getRoot();
    }
}
