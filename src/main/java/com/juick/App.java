package com.juick;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;

/**
 * Created by gerc on 14.02.2016.
 */
@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "support@juick.com", resSubject = R.string.appCrash)
public class App extends Application {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }
    }
}
