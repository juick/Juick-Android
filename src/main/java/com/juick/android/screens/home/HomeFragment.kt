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
package com.juick.android.screens.home

import android.net.Uri
import android.os.Bundle
import android.view.View
import com.juick.App
import com.juick.android.Uris
import com.juick.android.screens.FeedFragment
import isAuthenticated

/**
 * Created by gerc on 03.06.2016.
 */
class HomeFragment : FeedFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (App.instance.isAuthenticated) {
            if (vm.apiUrl.value == Uri.EMPTY) {
                vm.apiUrl.value = Uris.home
            }
        } else {
            if (vm.apiUrl.value == Uri.EMPTY) {
                vm.apiUrl.value = Uris.discussions
            }
        }
    }
}