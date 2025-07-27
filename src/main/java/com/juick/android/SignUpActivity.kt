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

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.databinding.ActivitySignupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var model: ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(model.root)
        val authCode = intent.getStringExtra("authCode")
        model.buttonCreate.setOnClickListener {
            val nick = model.newNick.text.toString()
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
    }
}