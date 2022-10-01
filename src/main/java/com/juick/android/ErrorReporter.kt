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
package com.juick.android

import android.content.Context
import java.lang.Thread.UncaughtExceptionHandler
import java.lang.Thread
import android.util.Log
import android.content.Intent
import android.net.Uri
import android.content.ComponentName

class ErrorReporter(
    private val context: Context,
    private val email: String,
    private val subject: String
) : UncaughtExceptionHandler {
    private val defaultHandler: UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            as UncaughtExceptionHandler

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        val stackTrace = Log.getStackTraceString(e)
        val intent = Intent(Intent.ACTION_SENDTO)
        val parameters = Uri.Builder()
            .appendQueryParameter("body", stackTrace)
            .appendQueryParameter("subject", subject)
            .build()
        val targetUri =
            Uri.parse(Uri.fromParts("mailto", email, null).toString() + parameters.toString())
        intent.data = targetUri
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolvedActivity = intent.resolveActivity(context.packageManager)
        if (resolvedActivity != null) {
            context.startActivity(intent)
        } else {
            Log.d(TAG, "Email client is not installed")
        }
        defaultHandler.uncaughtException(t, e)
    }

    companion object {
        private val TAG = ErrorReporter::class.java.simpleName
    }
}