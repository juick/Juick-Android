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

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.android.ui.AppTheme
import com.juick.android.ui.signup.SignUpScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authCode = intent.getStringExtra("authCode")

        setContent {
            AppTheme {
                SignUpScreen(
                    onSignUp = { nick ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val user = App.instance.api.signup(nick, authCode)
                                withContext(Dispatchers.Main) {
                                    val successIntent = Intent()
                                    successIntent.putExtra("nick", nick)
                                    successIntent.putExtra("hash", user.hash)
                                    setResult(RESULT_OK, successIntent)
                                    finish()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@SignUpActivity,
                                        "Username is not correct (already taken?)", Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
