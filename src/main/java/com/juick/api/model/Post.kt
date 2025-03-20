/*
 * Copyright (C) 2008-2024, Juick
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

import android.os.Parcelable
import com.juick.util.StringUtils
import com.stfalcon.chatkit.commons.models.IMessage
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

/**
 * Created by gerc on 10.02.2016.
 */
@Parcelize
@Serializable
data class Post(
    private var user: User,
    var mid: Int = 0,
    var rid: Int = 0,
    var subscribed: Boolean = false
) : IMessage, Parcelable {
    var replyto = 0
    var to: User? = null
    private var body: String? = null
    @Serializable(with = DateSerializer::class)
    private var timestamp: Date? = null
    var tags: List<String> = ArrayList()
    var replies = 0
    var likes = 0
    var repliesby: String? = null
    var replyQuote: String? = null
    @SerialName("attachment")
    var photo: Attachment? = null
    var friendsOnly: Boolean = false
    @SerialName("service")
    var isService = false

    @Transient
    var nextRid = 0

    @Transient
    var prevRid = 0
    val tagsString: String
        get() {
            val builder = StringBuilder()
            if (!tags.isEmpty()) {
                for (tag in tags) builder.append(" *").append(tag)
            }
            return builder.toString()
        }

    override fun getId(): String {
        return mid.toString()
    }

    override fun getText(): String {
        return body ?: ""
    }

    override fun getUser(): User {
        return user
    }

    override fun getCreatedAt(): Date {
        return timestamp ?: Date()
    }

    fun setUser(user: User) {
        this.user = user
    }

    fun getBody(): String? {
        return body
    }

    fun setBody(body: String?) {
        this.body = body
    }

    fun getTimestamp(): Date? {
        return timestamp
    }

    fun setTimestamp(timestamp: Date?) {
        this.timestamp = timestamp
    }

    companion object {
        fun empty(): Post {
            val post = Post(user = User(0, "Juick"))
            post.timestamp = Date()
            post.body = StringUtils.EMPTY
            return post
        }
    }
}

fun Post.isReply(): Boolean {
    return rid > 0
}