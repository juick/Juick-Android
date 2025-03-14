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
package com.juick.android.widget.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale

@SuppressLint("UseCompatLoadingForDrawables")
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
                    val downSampled = image.scale(32, 32, false)
                    val blurred = downSampled.scale(image.width, image.height, false)
                    setImageBitmap(blurred)
                } else {
                    setImageBitmap(image)
                }
            }
        } ?: run {
            withContext(Dispatchers.Main) {
                setImageDrawable(context.getDrawable(R.drawable.ei_refresh))
                tag = -1
            }
        }
    }
}

suspend fun loadImage(url: String): Bitmap? {
    if (url.isEmpty()) return null
    try {
        val bytes = App.instance.api.download(url).byteStream()
        return BitmapFactory.decodeStream(
            bytes
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}