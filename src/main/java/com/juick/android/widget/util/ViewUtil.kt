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
package com.juick.android.widget.util

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.juick.R

/**
 * Created by gerc on 14.02.2016.
 */

/**
 * android:drawableTint for API < 21
 */
fun TextView.setDrawableTint() {
    val ds = compoundDrawables
    val cs = arrayOfNulls<Drawable>(ds.size)
    val c = currentTextColor
    for (i in ds.indices) {
        if (ds[i] == null) {
            cs[i] = null
        } else {
            cs[i] = DrawableCompat.wrap(ds[i]!!)
            DrawableCompat.setTint(cs[i]!!, c)
        }
    }
    setCompoundDrawablesWithIntrinsicBounds(cs[0], cs[1], cs[2], cs[3])
}

fun hideKeyboard(activity: Activity?) {
    if (activity != null) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
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

fun LinearLayout.setCompatElevation(view: View?) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        val params = layoutParams as MarginLayoutParams
        params.bottomMargin = 0
        layoutParams = params
        val toolbarHeight = resources.getDimension(R.dimen.bottom_bar_elevation)
        view?.let { elevationView ->
            elevationView.visibility = View.VISIBLE
            val elevationParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, toolbarHeight.toInt()
            )
            elevationParams.bottomMargin = resources.getDimension(R.dimen.toolbar_elevation).toInt()
            elevationView.layoutParams = elevationParams
        }
    }
}
