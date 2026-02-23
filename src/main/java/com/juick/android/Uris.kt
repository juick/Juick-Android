/*
 * Copyright (C) 2008-2025, Juick
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

import android.net.Uri
import com.juick.BuildConfig

/**
 * Created by alx on 11.12.16.
 */
object Uris {
    val home: Uri
        get() = Utils.buildUrl(baseUri)
            .path("home")
            .clearQuery()
            .build()
    val photos: Uri
        get() = Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .appendQueryParameter("media", "all")
            .build()
    val last: Uri
        get() = Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .build()
    val top: Uri
        get() = Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .appendQueryParameter("popular", "1")
            .build()
    val discussions: Uri
        get() = Utils.buildUrl(baseUri)
            .path("messages/discussions")
            .clearQuery()
            .build()

    var baseUri: Uri = Uri.parse(BuildConfig.API_ENDPOINT)

    fun getUserPostsByName(uname: String): Uri {
        return Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .appendQueryParameter("uname", uname)
            .build()
    }

    fun getUserPostsByUid(uid: Int): Uri {
        return Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .appendQueryParameter("uid", "$uid")
            .build()
    }

    fun getPostsByTag(uid: Int, tag: String): Uri {
        return Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .appendQueryParameter("tag", tag)
            .build()
    }

    fun search(search: String): Uri {
        return Utils.buildUrl(baseUri)
            .path("messages")
            .clearQuery()
            .appendQueryParameter("search", search)
            .build()
    }
}