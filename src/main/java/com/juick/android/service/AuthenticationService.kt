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
package com.juick.android.service

import android.accounts.*
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.juick.R
import com.juick.android.SignInActivity
import com.juick.util.StringUtils

/**
 *
 * @author Ugnich Anton
 */
class AuthenticationService : Service() {
    private lateinit var accountAuthenticator: JuickAuthenticator
    override fun onBind(intent: Intent): IBinder? {
        val action = StringUtils.defaultString(intent.action)
        return if (action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            accountAuthenticator.iBinder
        } else null
    }

    override fun onCreate() {
        accountAuthenticator = JuickAuthenticator(applicationContext)
    }

    private class JuickAuthenticator(private val context: Context) :
        AbstractAccountAuthenticator(context) {
        @Throws(NetworkErrorException::class)
        override fun addAccount(
            response: AccountAuthenticatorResponse,
            accountType: String,
            authTokenType: String,
            requiredFeatures: Array<String>,
            options: Bundle
        ): Bundle {
            val i = Intent(context, SignInActivity::class.java)
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            val reply = Bundle()
            reply.putParcelable(AccountManager.KEY_INTENT, i)
            return reply
        }

        @Throws(NetworkErrorException::class)
        override fun getAuthToken(
            response: AccountAuthenticatorResponse,
            account: Account,
            authTokenType: String,
            options: Bundle
        ): Bundle {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, context.getString(R.string.com_juick))
            result.putString(
                AccountManager.KEY_AUTHTOKEN,
                AccountManager.get(context).getUserData(account, "hash")
            )
            return result
        }

        override fun hasFeatures(
            arg0: AccountAuthenticatorResponse,
            arg1: Account,
            arg2: Array<String>
        ): Bundle {
            TODO()
        }

        override fun updateCredentials(
            arg0: AccountAuthenticatorResponse,
            arg1: Account,
            arg2: String,
            arg3: Bundle
        ): Bundle {
            TODO()
        }

        override fun getAuthTokenLabel(arg0: String): String {
            TODO()
        }

        override fun confirmCredentials(
            arg0: AccountAuthenticatorResponse,
            arg1: Account,
            arg2: Bundle
        ): Bundle {
            TODO()
        }

        override fun editProperties(arg0: AccountAuthenticatorResponse, arg1: String): Bundle {
            TODO()
        }
    }
}