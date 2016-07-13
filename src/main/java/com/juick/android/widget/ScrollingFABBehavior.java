package com.juick.android.widget;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class ScrollingFABBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    private boolean enabled;

    public ScrollingFABBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        if (enabled) {
            if (dependency instanceof AppBarLayout) {
                if (ViewCompat.getY(dependency) > 0) {
                    fab.show();
                } else {
                    fab.hide();
                }
            }
        } else {
            fab.hide();
        }
        return true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
