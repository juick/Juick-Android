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

package com.juick.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.juick.App;
import com.juick.R;
import com.juick.databinding.ActivitySignupBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {
    private String authCode;

    private ActivitySignupBinding model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(model.getRoot());
        authCode = getIntent().getStringExtra("authCode");
        model.buttonCreate.setOnClickListener(v -> {
            String nick = model.newNick.getText().toString();
            String password = model.newPassword.getText().toString();
            String confirm = model.confirmPassword.getText().toString();
            if (!password.equals(confirm)) {
                Toast.makeText(this,"Passwords did not match", Toast.LENGTH_LONG).show();
                return;
            }
            App.getInstance().getApi().signup(nick, password, authCode).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Intent successIntent = new Intent();
                        successIntent.putExtra("nick", nick);
                        successIntent.putExtra("password", password);
                        setResult(RESULT_OK, successIntent);
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Username is not correct (already taken?)", Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(SignUpActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
