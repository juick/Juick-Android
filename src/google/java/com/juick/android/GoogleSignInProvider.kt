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

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.juick.App
import com.juick.R
import com.juick.util.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleSignInProvider : SignInProvider {
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var context: Activity
    override fun prepareSignIn(context: Activity, button: RelativeLayout): View? {
        this.context = context
        val googleClientId =
            StringUtils.defaultString(context.resources.getString(R.string.default_web_client_id))
        return if (TextUtils.isEmpty(googleClientId)) {
            null
        } else {
            // Configure sign-in to request the user's ID, email address, and basic
            // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleClientId)
                .requestEmail()
                .build()
            // Build a GoogleSignInClient with the options specified by gso.
            googleClient = GoogleSignIn.getClient(context, gso)

            // Set the dimensions of the sign-in button.
            val signInButton = SignInButton(context)
            signInButton.setSize(SignInButton.SIZE_STANDARD)
            signInButton.setColorScheme(SignInButton.COLOR_LIGHT)
            button.addView(signInButton)
            signInButton
        }
    }

    override fun onSignInResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        requestCallback: SignInRequestCallback,
        successCallback: SignInSuccessCallback
    ) {
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task, successCallback)
        }
        if (requestCode == RC_SIGN_UP) {
            val nick = data?.getStringExtra("nick") ?: ""
            val password = data?.getStringExtra("password") ?: ""
            requestCallback.invoke(nick, password)
        }
    }

    override fun performSignIn() {
        val signInIntent = googleClient.signInIntent
        context.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleSignInResult(
        completedTask: Task<GoogleSignInAccount>,
        successCallback: SignInSuccessCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val account = completedTask.getResult(ApiException::class.java)
                account.idToken?.let {
                    token ->
                    Log.i(SignInActivity::class.java.simpleName, "Success: $token")
                    val data = App.instance.api.googleAuth(token)
                    if (data.user == null) {
                        data.authCode?.let { authCode ->
                            Log.i(SignInActivity::class.java.simpleName, authCode)
                            withContext(Dispatchers.Main) {
                                val signupIntent =
                                    Intent(context, SignUpActivity::class.java)
                                signupIntent.putExtra("email", data.account)
                                signupIntent.putExtra("authCode", data.authCode)
                                context.startActivityForResult(signupIntent, RC_SIGN_UP)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            successCallback.invoke(data.user.name, data.user.hash!!)
                        }
                    }
                }
            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.w(
                    SignInActivity::class.java.simpleName,
                    "signInResult:failed code=" + e.statusCode
                )
            }
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val RC_SIGN_UP = 9002
    }
}