/*
 * Copyright (C) 2008-2023, Juick
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

package com.juick.android.screens.blog

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.juick.android.ProfileData
import com.juick.android.screens.FeedFragment

class BlogFragment : FeedFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = arguments?.getString("uname") ?: ProfileData.userProfile.value?.name ?: ""
        vm = ViewModelProvider(this, BlogViewModelFactory(name))[BlogViewModel::class.java]
        super.onViewCreated(view, savedInstanceState)
    }
}