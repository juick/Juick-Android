package com.juick.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by vt on 15/11/2017.
 */

public class UpdateReceiver extends BroadcastReceiver {
    public static class LegacyUpdateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent != null && intent.getData() != null && context.getPackageName().equals(intent.getData().getSchemeSpecificPart()))
            {
                onUpdate(context);
            }
        }
    }
    @Override
    public void onReceive(Context context, Intent intent)
    {
        onUpdate(context);
    }

    public static void onUpdate(Context context)
    {
        Log.d("Juick", "app is updated");
        context.stopService(new Intent(context, RegistrationIntentService.class));
        context.startService(new Intent(context, RegistrationIntentService.class));
    }
}
