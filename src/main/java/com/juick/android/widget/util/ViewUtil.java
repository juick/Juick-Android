package com.juick.android.widget.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.juick.R;

/**
 * Created by gerc on 14.02.2016.
 */
public class ViewUtil {

    public static void setTint(TextView v) {
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

    public static float pxToDp(float px) {
        float densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
        return px / (densityDpi / 160f);
    }

    public static int dpToPx(int dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
    }

    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }
}
