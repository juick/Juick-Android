package com.juick;

import android.app.Application;
import android.content.Context;
import android.util.TypedValue;

/**
 * Created by gerc on 14.02.2016.
 */
public class App extends Application {

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
