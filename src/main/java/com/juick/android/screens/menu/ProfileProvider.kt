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
package com.juick.android.screens.menu

import android.content.Context
import android.view.ActionProvider
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.juick.R
import com.juick.android.ProfileData
import com.juick.databinding.ProfileMenuLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileProvider(private val context: Context) : ActionProvider(context) {
    var binding: ProfileMenuLayoutBinding

    init {
        binding = ProfileMenuLayoutBinding.inflate(LayoutInflater.from(context))
        CoroutineScope(Dispatchers.IO).launch {
            ProfileData.userProfile.collect { user ->
                if (user.uid > 0) {
                    withContext(Dispatchers.Main) {
                        val profileImage = binding.profileImage
                        val avatarUrl: String = user.avatar
                        Glide.with(context)
                            .load(avatarUrl)
                            .placeholder(R.drawable.av_96)
                            .into(profileImage)
                    }
                }
            }
        }
    }

    override fun onCreateActionView(): View {
        return binding.root
    }

    override fun onCreateActionView(forItem: MenuItem): View {
        return binding.root
    }
}