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

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.juick.App
import com.juick.R
import com.juick.util.StringUtils
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleSignInProvider(): SignInProvider {
    private lateinit var context: ComponentActivity
    private lateinit var credentialManager: CredentialManager
    private lateinit var googleClientId: String
    private var signInContinuation: CancellableContinuation<Result<Bundle>>? = null
    private lateinit var signUpLauncher: ActivityResultLauncher<Intent>

    override fun prepareSignIn(context: ComponentActivity, container: RelativeLayout): View? {
        this.context = context
        signUpLauncher = context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (signInContinuation?.isCompleted == true) {
                return@registerForActivityResult
            }
            result.data?.extras?.let {
                signInContinuation?.resume(Result.success(it))
            } ?: run {
                signInContinuation?.resumeWithException(IllegalStateException("No signup data"))
            }
        }
        googleClientId = StringUtils.defaultString(context.resources.getString(R.string.default_web_client_id))
        return if (TextUtils.isEmpty(googleClientId)) {
            null
        } else {
            // Set the dimensions of the sign-in button.
            val signInButton = SignInButton(context)
            signInButton.setSize(SignInButton.SIZE_STANDARD)
            signInButton.setColorScheme(SignInButton.COLOR_LIGHT)
            container.addView(signInButton)
            signInButton
        }
    }

    override suspend fun performSignIn(): Result<Bundle> = suspendCancellableCoroutine { continuation ->
        signInContinuation = continuation
        credentialManager = CredentialManager.create(context)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleClientId)
            .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        context.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                val token = GoogleIdTokenCredential.createFrom(result.credential.data)
                Log.i(TAG, "Success: $token")
                val data = App.instance.api.googleAuth(token.idToken)
                if (data.user == null) {
                    data.authCode?.let { authCode ->
                        Log.i(SignInActivity::class.java.simpleName, authCode)
                        withContext(Dispatchers.Main) {
                            val signupIntent =
                                Intent(context, SignUpActivity::class.java)
                            signupIntent.putExtra("email", data.account)
                            signupIntent.putExtra("authCode", data.authCode)
                            signUpLauncher.launch(signupIntent)
                        }
                    }
                } else {
                    signInContinuation?.resume(Result.success(
                        bundleOf(
                            "nick" to data.user.name,
                            "hash" to (data.user.hash ?: "")
                        )
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google error", e)
                signInContinuation?.resumeWithException(e)
            }
        }
    }

    companion object {
        private val TAG = GoogleSignInProvider::class.simpleName
    }
}