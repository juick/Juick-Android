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

package com.juick.android.widget.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Created by gerc on 14.02.2016.
 */
public class ViewUtil {

    public static final int REQUEST_CODE_SYNC_CONTACTS = 5;

    /**
     * android:drawableTint for API < 21
     * @param v TextView
     */
    public static void setDrawableTint(TextView v) {
        if (v == null) return;
        Drawable[] ds = v.getCompoundDrawables();
        Drawable[] cs = new Drawable[ds.length];
        int c = v.getCurrentTextColor();
        for (int i = 0; i < ds.length; i++) {
            if (ds[i] == null) {
                cs[i] = null;
            } else {
                cs[i] = DrawableCompat.wrap(ds[i]);
                DrawableCompat.setTint(cs[i], c);
            }
        }
        v.setCompoundDrawablesWithIntrinsicBounds(cs[0], cs[1], cs[2], cs[3]);
    }

    public static void hideKeyboard(@Nullable Activity activity) {
        if (activity != null) {
            InputMethodManager imm =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
            }
        }
    }
}
