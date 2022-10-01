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

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.juick.App
import com.juick.R
import com.juick.databinding.ActivitySignupBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {
    private var authCode: String? = null
    private lateinit var model: ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(model.root)
        authCode = intent.getStringExtra("authCode")
        model.buttonCreate.setOnClickListener { v: View? ->
            val nick = model.newNick.text.toString()
            val password = model.newPassword.text.toString()
            val confirm = model.confirmPassword.text.toString()
            if (password != confirm) {
                Toast.makeText(this, "Passwords did not match", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            App.instance.api.signup(nick, password, authCode)!!.enqueue(object : Callback<Void?> {
                override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                    if (response.isSuccessful) {
                        val successIntent = Intent()
                        successIntent.putExtra("nick", nick)
                        successIntent.putExtra("password", password)
                        setResult(RESULT_OK, successIntent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@SignUpActivity,
                            "Username is not correct (already taken?)", Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void?>, t: Throwable) {
                    Toast.makeText(this@SignUpActivity, R.string.network_error, Toast.LENGTH_LONG)
                        .show()
                }
            })
        }
    }
}