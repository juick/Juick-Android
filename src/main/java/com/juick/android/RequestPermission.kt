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

package com.juick.android

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.juick.App
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class RequestPermission(activity: ComponentActivity,
                        private val permission: String, private val sdk: Int) {
    private val applicationContext = App.instance
    private var requestPermissionContinuation: CancellableContinuation<Boolean>? = null
    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (requestPermissionContinuation?.isCompleted == true) {
                return@registerForActivityResult
            }
            requestPermissionContinuation?.resumeWith(Result.success(isGranted))
        }

    suspend operator fun invoke() = suspendCancellableCoroutine { continuation ->
        requestPermissionContinuation = continuation
        if (Build.VERSION.SDK_INT < sdk) {
            if (requestPermissionContinuation?.isCompleted == true) {
                return@suspendCancellableCoroutine
            }
            requestPermissionContinuation?.resumeWith(Result.success(true))
            return@suspendCancellableCoroutine
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED) {
            if (requestPermissionContinuation?.isCompleted == true) {
                return@suspendCancellableCoroutine
            }
            requestPermissionContinuation?.resumeWith(Result.success(true))
            return@suspendCancellableCoroutine
        }
        requestPermissionLauncher.launch(permission)
        continuation.invokeOnCancellation {
            requestPermissionContinuation = null
        }
    }
}
