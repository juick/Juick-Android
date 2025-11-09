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
package com.juick.android

import android.content.Context
import android.net.Uri
import android.util.Log
import com.juick.App
import com.juick.api.model.ExternalToken
import isAuthenticated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.net.URISyntaxException

/**
 *
 * @author Ugnich Anton
 */
object Utils {
    private var eventsFactoryInstance: OkHttpClient.Builder? = null
    val eventsFactory: OkHttpClient.Builder?
        get() {
            if (eventsFactoryInstance == null) {
                eventsFactoryInstance = OkHttpClient.Builder()
            }
            return eventsFactoryInstance
        }

    fun isImageTypeAllowed(mime: String): Boolean {
        return mime == "image/jpeg" || mime == "image/png"
    }

    fun getMimeTypeFor(context: Context, url: Uri): String? {
        val resolver = context.contentResolver
        return resolver.getType(url)
    }

    fun updateToken(tokenType: String, prefToken: String) {
        if (App.instance.isAuthenticated) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    App.instance.api.registerPush(
                        listOf(
                            ExternalToken(
                                type = tokenType,
                                token = prefToken
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.d(NotificationSender.TAG, "Failed to register", e)
                }
            }
        }
    }

    @Throws(URISyntaxException::class)
    fun buildUrl(uri: Uri): Uri.Builder {
        return Uri.Builder()
            .scheme(uri.scheme)
            .authority(uri.authority)
            .path(uri.path)
            .encodedQuery(uri.encodedQuery)
    }
    fun Uri.replaceUriParameter(key: String, newValue: String): Uri {
        val params = queryParameterNames
        val newUri = buildUpon().clearQuery()
        var isSameParamPresent = false
        for (param in params) {
            // if same param is present override it, otherwise add the old param back
            newUri.appendQueryParameter(param,
                if (param == key) newValue else getQueryParameter(param))
            if (param == key) {
                // make sure we do not add new param again if already overridden
                isSameParamPresent = true
            }
        }
        if (!isSameParamPresent) {
            // never overrode same param so add new passed value now
            newUri.appendQueryParameter(key,
                newValue)
        }
        return newUri.build()
    }
}