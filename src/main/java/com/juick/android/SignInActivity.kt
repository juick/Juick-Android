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

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.R
import com.juick.android.Utils.hasAuth
import com.juick.databinding.ActivityLoginBinding
import com.juick.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author Ugnich Anton
 */
class SignInActivity : AppCompatActivity() {
    enum class SignInStatus {
        SIGNED_OUT, SIGN_IN_PROGRESS, SIGNED_IN
    }

    private var authenticatorResponse: AccountAuthenticatorResponse? = null
    private var currentAction = 0
    private lateinit var model: ActivityLoginBinding
    private val application = App.instance
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticatorResponse =
            intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()
        model = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(model.root)
        model.buttonSave.setOnClickListener {
            val nick = model.juickNick.text.toString()
            val password = model.juickPassword.text.toString()
            if (nick.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this@SignInActivity,
                    R.string.Enter_nick_and_password,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val me = application.auth(nick, password)
                    withContext(Dispatchers.Main) {
                        updateAccount(nick, me.hash ?: "", currentAction)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SignInActivity,
                            R.string.Unknown_nick_or_wrong_password,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        application.signInProvider?.prepareSignIn(this, model.signInButtonPlaceholder)
            ?.let { signInButton ->
                signInButton.setOnClickListener { application.signInProvider?.performSignIn() }
            }
        currentAction = intent.getIntExtra(EXTRA_ACTION, ACTION_ACCOUNT_CREATE)
        if (hasAuth() && currentAction != ACTION_PASSWORD_UPDATE) {
            val builder = AlertDialog.Builder(this)
            builder.setNeutralButton(android.R.string.ok) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            builder.setMessage(R.string.Only_one_account)
            builder.show()
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        application.signInProvider?.onSignInResult(
            requestCode,
            resultCode,
            data,
            { nick: String?, password: String? ->
                if (!TextUtils.isEmpty(nick)) {
                    model.juickNick.setText(nick)
                    model.juickPassword.setText(password)
                    model.buttonSave.performClick()
                }
            }) { username: String, hash: String ->
            updateAccount(
                username,
                hash,
                ACTION_ACCOUNT_CREATE
            )
        }
    }

    private fun updateAccount(nick: String, hash: String, action: Int) {
        val account = Account(nick, getString(R.string.com_juick))
        val am = AccountManager.get(this@SignInActivity)
        if (action == ACTION_PASSWORD_UPDATE) {
            am.setAuthToken(account, StringUtils.EMPTY, hash)
        } else {
            val userData = Bundle()
            userData.putString("hash", hash)
            am.addAccountExplicitly(account, StringUtils.EMPTY, userData)
        }
        val result = Bundle()
        result.putString(AccountManager.KEY_ACCOUNT_NAME, nick)
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.com_juick))
        result.putString(AccountManager.KEY_AUTHTOKEN, hash)
        authenticatorResponse?.onResult(result)
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val ACTION_ACCOUNT_CREATE = 0
        const val ACTION_PASSWORD_UPDATE = 1
    }
}