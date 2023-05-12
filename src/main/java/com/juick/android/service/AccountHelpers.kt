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

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import com.juick.App
import com.juick.R
import com.juick.util.StringUtils

private val App.account: Account?
    get() {
        val am = AccountManager.get(this)
        val accounts = am.getAccountsByType(App.instance.getString(R.string.com_juick))
        return accounts.firstOrNull()
    }

val App.isAuthenticated: Boolean
    get() {
        return account != null
    }

val App.accountData: String
    get() {
        if (account != null) {
            try {
                val am = AccountManager.get(this)
                val b = am.getAuthToken(account, StringUtils.EMPTY, null, false, null, null).result
                return b.getString(AccountManager.KEY_AUTHTOKEN, StringUtils.EMPTY)
            } catch (e: Exception) {
                Log.d("getBasicAuthString", Log.getStackTraceString(e))
            }
        }
        return StringUtils.EMPTY
    }