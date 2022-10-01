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

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.juick.App
import com.juick.R
import com.juick.util.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 *
 * @author Ugnich Anton
 */
object Utils {
    @JvmStatic
    var myId = 0
    @JvmStatic
    val account: Account?
        get() {
            val am = AccountManager.get(App.getInstance())
            val accounts = am.getAccountsByType(App.getInstance().getString(R.string.com_juick))
            return accounts.firstOrNull()
        }

    @JvmStatic
    fun hasAuth(): Boolean {
        return account != null
    }

    val nick: String?
        get() {
            val account = account
            return account?.name
        }
    @JvmStatic
    val accountData: Bundle?
        get() {
            val am = AccountManager.get(App.getInstance())
            val account = account
            if (account != null) {
                var b: Bundle? = null
                try {
                    b = am.getAuthToken(account, StringUtils.EMPTY, null, false, null, null).result
                } catch (e: Exception) {
                    Log.d("getBasicAuthString", Log.getStackTraceString(e))
                }
                return b
            }
            return null
        }
    private var eventsFactoryInstance: OkHttpClient.Builder? = null
    @JvmStatic
    val eventsFactory: OkHttpClient.Builder?
        get() {
            if (eventsFactoryInstance == null) {
                eventsFactoryInstance = OkHttpClient.Builder()
            }
            return eventsFactoryInstance
        }

    @JvmStatic
    fun isImageTypeAllowed(mime: String?): Boolean {
        return mime != null && (mime == "image/jpeg" || mime == "image/png")
    }

    @JvmStatic
    fun getMimeTypeFor(context: Context, url: Uri?): String? {
        val resolver = context.contentResolver
        return resolver.getType(url!!)
    }

    @JvmStatic
    fun updateFCMToken(prefToken: String?) {
        val TAG = "updateFCMToken"
        Log.d(TAG, "currentToken $prefToken")
        if (hasAuth()) {
            if (prefToken != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        App.getInstance().api.registerPush(prefToken)
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to register", e)
                    }
                }
            }
        }
    }
}