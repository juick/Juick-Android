/*
 * Copyright (C) 2008-2026, Juick
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
package com.juick.android.testing

import android.net.Uri
import com.juick.android.Uris
import com.juick.android.Utils
import com.juick.android.Utils.replaceUriParameter
import org.junit.Assert.*
import org.junit.Test

class UrisTest {
    @Test
    fun top_hasPopularParam() {
        val uri = Uris.top
        assertEquals("https", uri.scheme)
        assertEquals("api.juick.com", uri.authority)
        assertEquals("/messages", uri.path)
        assertEquals("1", uri.getQueryParameter("popular"))
    }

    @Test
    fun last_noQueryParams() {
        val uri = Uris.last
        assertEquals("/messages", uri.path)
        assertNull(uri.getQueryParameter("popular"))
    }

    @Test
    fun home_path() {
        assertEquals("/home", Uris.home.path)
    }

    @Test
    fun pagination_preservesPopular() {
        val initial = Uris.top
        val next = Utils.buildUrl(initial).build()
            .replaceUriParameter("before_mid", "12345")
            .replaceUriParameter("ts", "999")
        assertEquals("1", next.getQueryParameter("popular"))
        assertEquals("12345", next.getQueryParameter("before_mid"))
        assertEquals("999", next.getQueryParameter("ts"))
        assertEquals("/messages", next.path)
    }

    @Test
    fun pagination_addsBeforeMid() {
        val initial = Uris.last
        val next = Utils.buildUrl(initial).build()
            .replaceUriParameter("before_mid", "12345")
        assertEquals("12345", next.getQueryParameter("before_mid"))
        assertEquals("/messages", next.path)
    }

    @Test
    fun replaceUriParameter_updatesExisting() {
        val uri = Uri.parse("https://api.juick.com/messages?before_mid=111")
        val updated = uri.replaceUriParameter("before_mid", "999")
        assertEquals("999", updated.getQueryParameter("before_mid"))
        assertEquals("/messages", updated.path)
    }
}
