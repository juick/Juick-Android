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

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.juick.R
import com.juick.android.ui.AppTheme
import com.juick.android.ui.standardSpacing

class NextSignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignInView()
        }
    }

    @Composable
    fun SignInView() {
        var nick by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AppTheme {
            Scaffold { contentPadding ->
                Box(modifier = Modifier.padding(contentPadding)) {
                    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                        val (logo, nickField, passwordField, submitButton) = createRefs()
                        Image(
                            modifier = Modifier.constrainAs(logo) {
                                centerTo(parent)
                            },
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = "Main logo"
                        )
                        TextField(
                            modifier = Modifier.constrainAs(nickField) {
                                top.linkTo(logo.bottom, margin = standardSpacing)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }, value = nick, onValueChange = { nick = it })
                        TextField(
                            modifier = Modifier.constrainAs(passwordField) {
                                top.linkTo(nickField.bottom, margin = standardSpacing)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            },
                            value = password, onValueChange = { password = it },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Password
                            ),
                        )
                        Button(
                            modifier = Modifier.constrainAs(submitButton) {
                                top.linkTo(passwordField.bottom, standardSpacing)
                                start.linkTo(passwordField.start)
                                end.linkTo(passwordField.end)
                            },
                            onClick = {
                                if (nick.isEmpty() || password.isEmpty()) {
                                    Toast.makeText(
                                        this@NextSignInActivity,
                                        R.string.Enter_nick_and_password,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                            }, shape = RectangleShape) {
                            Text(stringResource(R.string.login))
                        }
                    }
                }
            }
        }
    }

    @Preview(apiLevel = 34)
    @Composable
    fun PreviewSignInView() {
        SignInView()
    }
}