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
package com.juick.android

import com.juick.App
import com.juick.api.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

object ProfileData {
    val anonymous = User(uid = 0, uname = "Anonymous")
    var userProfile: MutableStateFlow<User?> = MutableStateFlow(null)
        private set

    suspend fun refresh() {
        userProfile.update {
            null
        }
        try {
            userProfile.value = withContext(Dispatchers.IO) {
                App.instance.api.me()
            }
        } catch (e: Exception) {
            userProfile.value = anonymous
        }
    }
}