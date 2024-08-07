/*
 * Copyright (C) 2008-2024, Juick
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

package com.juick.android.updater

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.widget.util.getLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException

@Serializable
data class Asset(
    val name: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

@Serializable
data class Release(
    val name: String,
    val body: String,
    val url: String,
    val assets: List<Asset>,
    @SerialName("tag_name") val tagName: String
)

private const val FILENAME_APK = "update.apk"
private const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"

class Updater(private val activity: Activity) {
    @Throws(
        UnknownHostException::class,
        InterruptedIOException::class
    )
    suspend fun download(url: String, filename: String): File? {
        try {
            val responseBody = App.instance.api.download(url)
            val file = File(activity.applicationContext.getFileStreamPath(filename).path)
            val sink: BufferedSink = file.sink().buffer()
            sink.writeAll(responseBody.source())
            sink.close()
            return file
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun checkUpdate() {
        try {
            val update = withContext(Dispatchers.IO) { App.instance.api.releases() }.firstOrNull {
                Version(it.name) > Version(BuildConfig.VERSION_NAME)
            }
            if (update != null) {
                showUpdateAvailable(update)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Update check failed: ${e.message}")
        }
    }

    private fun showUpdateAvailable(release: Release) {
        val downloadUrl = release.assets.filter {
            it.contentType == CONTENT_TYPE_APK
                    && it.name.contains(BuildConfig.FLAVOR)
        }.map { it.browserDownloadUrl }.firstOrNull() ?: return
        activity.getLifecycleOwner()?.lifecycleScope?.launch {
            AlertDialog.Builder(activity)
                .setTitle(release.name)
                .setMessage(release.body)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> updateApp(downloadUrl) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun updateApp(downloadUrl: String) {
        downloadUpdate(downloadUrl)
    }

    private fun downloadUpdate(url: String) {
        Toast.makeText(activity, activity.getString(R.string.downloading_update), Toast.LENGTH_LONG).show()
        val scope = activity.getLifecycleOwner()?.lifecycleScope
        scope?.launch(Dispatchers.IO) {
            try {
                val apkFile = download(url, FILENAME_APK)
                if (apkFile != null) {
                    scope.launch {
                        installAPK(apkFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("Updater", "Update error: $e.message", e)
                scope.launch {
                    Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installAPK(apkFile: File) {
        try {
            val uri =
                FileProvider.getUriForFile(activity, activity.packageName + ".provider", apkFile)
            val i = Intent(Intent.ACTION_VIEW)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            i.setDataAndType(uri, CONTENT_TYPE_APK)
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.startActivity(i)
        } catch (e: Exception) {
            Log.d("Updater", "Update error: $e.message")
            Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
        }
    }
    companion object {
        private val TAG = Updater::class.java.simpleName
    }
}