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
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.annotation.DrawableRes
import androidx.lifecycle.lifecycleScope
import com.juick.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun ImageView.load(url: String, scaleToScreenDensity: Boolean = true, blur: Boolean = false) {
    val scope = context.getLifecycleOwner()?.lifecycleScope
    scope?.launch {
        loadImage(url)?.let { image ->
            withContext(Dispatchers.Main) {
                if (scaleToScreenDensity) {
                    image.density = DisplayMetrics.DENSITY_DEFAULT
                    if (width < image.width) {
                        scaleType = ScaleType.CENTER_INSIDE
                    }
                }
                if (blur) {
                    val downSampled = Bitmap.createScaledBitmap(image, 32, 32, false)
                    val blurred =
                        Bitmap.createScaledBitmap(downSampled, image.width, image.height, false)
                    setImageBitmap(blurred)
                } else {
                    setImageBitmap(image)
                }
            }
        }
    }
}

suspend fun loadImage(url: String): Bitmap? {
    if (url.isEmpty()) return null
    return BitmapFactory.decodeStream(
        App.instance.api.download(url).byteStream()
    )
}