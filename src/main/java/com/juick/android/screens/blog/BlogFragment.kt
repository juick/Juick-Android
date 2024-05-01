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
import com.juick.android.UrlBuilder
import com.juick.android.screens.FeedFragment

class BlogFragment : FeedFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uname = arguments?.getString("uname") ?: ""
        val blog = uname.ifEmpty { account.profile.value?.name ?: "" }
        if (vm.apiUrl.value.isEmpty()) {
            vm.apiUrl.value = UrlBuilder.getUserPostsByName(blog).toString()
        }
    }
}