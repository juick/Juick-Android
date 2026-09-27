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
package com.juick.android.ui.screens.feed

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.size.Size
import coil3.transform.Transformation

class PixelateTransformation(private val blocks: Int = 32) : Transformation() {
    override val cacheKey: String = "pixelate-$blocks"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap =
        input.scale(blocks, blocks, false).scale(input.width, input.height, false)
}
