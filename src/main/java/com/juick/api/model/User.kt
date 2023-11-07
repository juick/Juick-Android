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
package com.juick.api.model

import com.stfalcon.chatkit.commons.models.IUser

/**
 * Created by gerc on 10.02.2016.
 */
data class User(
    val uid: Int,
    val uname: String
) : IUser {
    var unreadCount: Int = 0
    val premium: Boolean = false
    val admin: Boolean = false
    val vip: List<User> = listOf()
    val ignored: List<User> = listOf()
    var hash: String? = null
    var fullname: String? = null
    var isBanned = false
    private var avatar = ""
    var read: List<User>? = null

    override fun getId(): String {
        return uname
    }

    override fun getName(): String {
        return uname
    }

    override fun getAvatar(): String {
        return avatar
    }
}