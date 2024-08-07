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
package com.juick.util

import okio.buffer
import okio.source
import java.io.IOException
import java.io.InputStream


object StringUtils {
    const val EMPTY = ""
    fun defaultString(str: String?): String {
        return str ?: EMPTY
    }
}

/**
 * Reads InputStream and returns a String. It will close stream after usage.
 *
 * @param stream the stream to read
 * @return the string content
 */
@Throws(IOException::class)
fun InputStream.getString(): String {
    source().buffer().use { source ->
        return source.readUtf8()
    }
}