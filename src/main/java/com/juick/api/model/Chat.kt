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

import com.fasterxml.jackson.annotation.JsonFormat
import com.stfalcon.chatkit.commons.models.IDialog
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*

/**
 * Created by gerc on 11.03.2016.
 */
class Chat : IDialog<Post> {
    var uname: String? = null
    var uid = 0
    var avatar: String? = null
    var messagesCount = 0

    @get:JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss",
        timezone = "UTC"
    )
    var lastMessageTimestamp: Date? = null
    var lastMessageText: String? = null
    private var lastMessage: Post? = null
    override fun getId(): String {
        return uid.toString()
    }

    override fun getDialogPhoto(): String {
        return avatar!!
    }

    override fun getDialogName(): String {
        return uname!!
    }

    override fun getUsers(): List<IUser> {
        return listOf(User(uid, uname))
    }

    override fun getLastMessage(): Post {
        if (lastMessage != null) {
            return lastMessage as Post
        }
        val dummyPost: Post = Post.empty()
        dummyPost.setUser(users[0] as User)
        dummyPost.setBody(lastMessageText)
        dummyPost.setTimestamp(lastMessageTimestamp)
        return dummyPost
    }

    override fun setLastMessage(message: Post) {
        lastMessage = message
    }

    override fun getUnreadCount(): Int {
        return messagesCount
    }
}