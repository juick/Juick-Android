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
package com.juick.android.updater

class Version(version: String) : Comparable<Version> {
    val numbers: IntArray

    init {
        val split = version.split(".").toTypedArray()
        numbers = IntArray(split.size)
        for (i in split.indices) {
            numbers[i] = Integer.valueOf(split[i])
        }
    }

    override operator fun compareTo(other: Version): Int {
        val maxLength = Math.max(numbers.size, other.numbers.size)
        for (i in 0 until maxLength) {
            val left = if (i < numbers.size) numbers[i] else 0
            val right = if (i < other.numbers.size) other.numbers[i] else 0
            if (left != right) {
                return if (left < right) -1 else 1
            }
        }
        return 0
    }
}