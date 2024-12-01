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
package com.juick.android.widget.util

import android.graphics.Bitmap
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation


class BlurTransformation : Transformation() {

    override val cacheKey: String
        get() = javaClass.name

    override fun equals(other: Any?): Boolean {
        return other is BlurTransformation
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode()
    }

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val downSampled = Bitmap.createScaledBitmap(input, 30, 30, false)
        return Bitmap.createScaledBitmap(downSampled, size.width.pxOrElse { 320 }, size.height.pxOrElse { 240 }, false)
    }
}