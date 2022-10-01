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
package com.juick.android.widget.util

import android.widget.TextView
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import android.app.Activity
import android.view.inputmethod.InputMethodManager
import android.content.Context

/**
 * Created by gerc on 14.02.2016.
 */
object ViewUtil {
    const val REQUEST_CODE_SYNC_CONTACTS = 5

    /**
     * android:drawableTint for API < 21
     * @param v TextView
     */
    fun setDrawableTint(v: TextView?) {
        if (v == null) return
        val ds = v.compoundDrawables
        val cs = arrayOfNulls<Drawable>(ds.size)
        val c = v.currentTextColor
        for (i in ds.indices) {
            if (ds[i] == null) {
                cs[i] = null
            } else {
                cs[i] = DrawableCompat.wrap(ds[i]!!)
                DrawableCompat.setTint(cs[i]!!, c)
            }
        }
        v.setCompoundDrawablesWithIntrinsicBounds(cs[0], cs[1], cs[2], cs[3])
    }

    fun hideKeyboard(activity: Activity?) {
        if (activity != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
    }
}