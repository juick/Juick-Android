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

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.juick.R

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

/**
 * Toolbar elevation for API < 21
 *
 * @param appBarLayout AppBarLayout
 */
fun Activity.setAppBarElevation(appBarLayout: AppBarLayout) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        val toolbarHeight = resources.getDimension(R.dimen.toolbar_elevation)
        val elevationView = View(this)
        elevationView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, toolbarHeight.toInt()
        )
        ViewCompat.setBackground(elevationView,
            ContextCompat.getDrawable(this, R.drawable.elevation_pre_lollipop))
        appBarLayout.addView(elevationView, 1)
    }
}
