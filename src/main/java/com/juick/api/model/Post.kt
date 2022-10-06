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
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.juick.util.StringUtils
import com.stfalcon.chatkit.commons.models.IMessage
import java.util.*

/**
 * Created by gerc on 10.02.2016.
 */
data class Post(private var user: User) : IMessage {
    var mid = 0
    var replyto = 0
    var to: User? = null
    private var body: String? = null
    private var timestamp: Date? = null
    var tags: List<String> = ArrayList()
    var replies = 0
    var likes = 0
    var repliesby: String? = null
    var replyQuote: String? = null
    var photo: Photo? = null
    var rid = 0
    @JsonProperty("service")
    var isService = false

    @JsonIgnore
    var nextRid = 0

    @JsonIgnore
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
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