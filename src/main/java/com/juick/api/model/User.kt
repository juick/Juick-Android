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
package com.juick.api.model

import com.stfalcon.chatkit.commons.models.IUser

/**
 * Created by gerc on 10.02.2016.
 */
data class User(val uid: Int, val uname: String) : IUser {
    var hash: String? = null
    var fullname: String? = null
    var isBanned = false
    private var avatar: String? = null
    var unreadCount = 0
    var read: List<User>? = null

    override fun getId(): String {
        return uid.toString()
    }

    override fun getName(): String {
        return uname
    }

    override fun getAvatar(): String {
        return avatar ?: ""
    }
}