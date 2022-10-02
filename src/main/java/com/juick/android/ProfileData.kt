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
import com.juick.api.model.SecureUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

object ProfileData {
    val anonymous : SecureUser
    get() {
        val user = SecureUser()
        user.uid = 0
        user.uname = "Anonymous"
        return user
    }
    var userProfile = MutableStateFlow(anonymous)
        private set

    suspend fun refresh() {
        try {
            userProfile.value = withContext(Dispatchers.IO) {
                App.instance.api.me()
            }
        } catch (e: Exception) {
            userProfile.value = anonymous
        }
    }
}