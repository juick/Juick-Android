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

package com.juick.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

public class ErrorReporter implements Thread.UncaughtExceptionHandler {
    private static final String TAG = ErrorReporter.class.getSimpleName();
    private final Context context;
    private final String email;
    private final String subject;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public ErrorReporter(Context appContext, String developerEmail, String emailSubject) {
        this.context = appContext;
        this.email = developerEmail;
        this.subject = emailSubject;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        String stackTrace = Log.getStackTraceString(e);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        Uri parameters = new Uri.Builder()
                .appendQueryParameter("body", stackTrace)
                .appendQueryParameter("subject", subject)
                .build();
        Uri targetUri = Uri.parse(Uri.fromParts("mailto", email, null).toString() + parameters.toString());
        intent.setData(targetUri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final ComponentName resolvedActivity = intent.resolveActivity(context.getPackageManager());
        if (resolvedActivity != null) {
            context.startActivity(intent);
        } else {
            Log.d(TAG, "Email client is not installed");
        }
        defaultHandler.uncaughtException(t, e);
    }
}
