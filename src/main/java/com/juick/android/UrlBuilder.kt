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

import android.os.Parcelable
import android.os.Parcel
import java.net.URLEncoder
import java.io.UnsupportedEncodingException
import android.os.Parcelable.Creator

/**
 * Created by alx on 11.12.16.
 */
class UrlBuilder : Parcelable {
    var url: String? = null

    private constructor() {}
    private constructor(u: String?) {
        url = u
    }

    override fun toString(): String {
        return url!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
    }

    companion object {
        val home: UrlBuilder
            get() {
            val url = UrlBuilder()
            url.url = "/home?1=1"
            return url
        }

        val photos: UrlBuilder
            get() {
                val url = UrlBuilder()
                url.url = "messages?media=all"
                return url
            }
        val last: UrlBuilder
            get() {
                val url = UrlBuilder()
                url.url = "messages?1=1"
                return url
            }
        val top: UrlBuilder
            get() {
                val url = UrlBuilder()
                url.url = "messages?popular=1"
                return url
            }

        fun getUserPostsByName(uname: String): UrlBuilder {
            val url = UrlBuilder()
            url.url = "messages?uname=$uname"
            return url
        }

        fun getUserPostsByUid(uid: Int): UrlBuilder {
            val url = UrlBuilder()
            url.url = "messages?uid=$uid"
            return url
        }

        fun getPostsByTag(uid: Int, tag: String?): UrlBuilder {
            val url = UrlBuilder()
            try {
                url.url = "messages?tag=" + URLEncoder.encode(tag, "utf-8")
            } catch (ex: UnsupportedEncodingException) {
            }
            return url
        }

        val discussions: UrlBuilder
            get() {
                val builder = UrlBuilder()
                builder.url = "messages/discussions"
                return builder
            }
        @JvmField
        val CREATOR: Creator<UrlBuilder?> = object : Creator<UrlBuilder?> {
            override fun createFromParcel(input: Parcel): UrlBuilder {
                return UrlBuilder(input.readString())
            }

            override fun newArray(size: Int): Array<UrlBuilder?> {
                return arrayOfNulls(size)
            }
        }
    }
}