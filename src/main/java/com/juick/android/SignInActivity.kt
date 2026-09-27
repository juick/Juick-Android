/*
 * Copyright (C) 2008-2026, Juick
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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.R
import com.juick.android.service.account
import com.juick.android.service.accountData
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.AppTheme
import com.juick.android.ui.signin.SignInScreen
import com.juick.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author Ugnich Anton
 */
class SignInActivity : ComponentActivity() {
    enum class SignInStatus {
        SIGNED_OUT, SIGN_IN_PROGRESS, SIGNED_IN
    }

    private var authenticatorResponse: AccountAuthenticatorResponse? = null
    private var currentAction = 0
    private val application = App.instance
    private lateinit var signUpLauncher: ActivityResultLauncher<Intent>
    private var googleSignInButton: android.view.View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signUpLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.extras?.let {
                    updateAccount(it.getString("nick", ""),
                        it.getString("hash", ""), currentAction)
                } ?: run {
                    Toast.makeText(
                        this@SignInActivity, R.string.Error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        authenticatorResponse =
            intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        authenticatorResponse?.onRequestContinued()
        currentAction = intent.getIntExtra(EXTRA_ACTION, ACTION_ACCOUNT_CREATE)

        // Prepare Google sign-in button (must be done before setContent)
        val placeholder = android.widget.RelativeLayout(this)
        application.signInProvider?.prepareSignIn(this, placeholder)
            ?.let { signInButton ->
                googleSignInButton = signInButton
                signInButton.setOnClickListener {
                    lifecycleScope.launch {
                        try {
                            application.signInProvider?.performSignIn()?.fold(
                                onSuccess = { bundle ->
                                    val nick = bundle.getString("nick") ?: ""
                                    val account = bundle.getString("email") ?: ""
                                    val authCode = bundle.getString("authCode")
                                    val hash = bundle.getString("hash") ?: ""
                                    authCode?.let {
                                        val signupIntent =
                                            Intent(this@SignInActivity, SignUpActivity::class.java)
                                        signupIntent.putExtra("email", account)
                                        signupIntent.putExtra("authCode", authCode)
                                        signUpLauncher.launch(signupIntent)
                                    } ?: run {
                                        updateAccount(nick, hash, currentAction)
                                    }
                                },
                                onFailure = {
                                    Toast.makeText(
                                        this@SignInActivity,
                                        it.localizedMessage, Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@SignInActivity,
                                e.localizedMessage, Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

        val initialNick = if (currentAction == ACTION_PASSWORD_UPDATE) {
            App.instance.account?.name ?: ""
        } else {
            ""
        }

        if (App.instance.isAuthenticated && currentAction != ACTION_PASSWORD_UPDATE) {
            val builder = AlertDialog.Builder(this)
            builder.setNeutralButton(android.R.string.ok) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            builder.setMessage(R.string.Only_one_account)
            builder.show()
        }

        setContent {
            AppTheme {
                SignInScreen(
                    currentAction = currentAction,
                    initialNick = initialNick,
                    googleSignInButton = googleSignInButton,
                    onSignIn = { nick, password ->
                        if (nick.isEmpty() || password.isEmpty()) {
                            Toast.makeText(
                                this,
                                R.string.Enter_nick_and_password,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@SignInScreen
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
                                        e.localizedMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun updateAccount(nick: String, hash: String, action: Int) {
        val am = AccountManager.get(this@SignInActivity)
        if (action == ACTION_PASSWORD_UPDATE) {
            val account = App.instance.account
            account?.let {
                am.invalidateAuthToken(account.type, App.instance.accountData)
                am.setAuthToken(account, StringUtils.EMPTY, hash)
            } ?: run {
                Log.d("Auth", "Account missing")
            }
        } else {
            val account = Account(nick, getString(R.string.applicationId))
            val userData = Bundle()
            userData.putString("hash", hash)
            am.addAccountExplicitly(account, StringUtils.EMPTY, userData)
        }
        val result = Bundle()
        result.putString(AccountManager.KEY_ACCOUNT_NAME, nick)
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.applicationId))
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
