/*
 * Copyright (C) 2008-2020, Juick
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

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.juick.App;
import com.juick.R;
import com.juick.api.model.AuthResponse;
import com.juick.util.StringUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoogleSignInProvider implements SignInProvider {

    private GoogleSignInClient googleClient;

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SIGN_UP = 9002;

    private Activity context;

    @Override
    @Nullable
    public View prepareSignIn(Activity context, RelativeLayout button) {
        this.context = context;
        String googleClientId = StringUtils.defaultString(context.getResources().getString(R.string.default_web_client_id));

        if (TextUtils.isEmpty(googleClientId)) {
            return null;
        } else {
            // Configure sign-in to request the user's ID, email address, and basic
            // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(googleClientId)
                    .requestEmail()
                    .build();
            // Build a GoogleSignInClient with the options specified by gso.
            googleClient = GoogleSignIn.getClient(context, gso);

            // Set the dimensions of the sign-in button.
            SignInButton signInButton = new SignInButton(context);
            signInButton.setSize(SignInButton.SIZE_STANDARD);
            signInButton.setColorScheme(SignInButton.COLOR_LIGHT);
            button.addView(signInButton);
            return signInButton;
        }
    }

    @Override
    public void onSignInResult(int requestCode, int resultCode, Intent data, SignInRequestCallback requestCallback, SignInSuccessCallback successCallback) {
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task, successCallback);
        }
        if (requestCode == RC_SIGN_UP && data != null) {
            String nick = data.getStringExtra("nick");
            String password = data.getStringExtra("password");
            requestCallback.request(nick, password);
        }
    }

    @Override
    public void performSignIn() {
        Intent signInIntent = googleClient.getSignInIntent();
        context.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask, SignInSuccessCallback successCallback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            Log.i(SignInActivity.class.getSimpleName(), "Success " + account.getIdToken());
            App.getInstance().getApi().googleAuth(account.getIdToken()).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                    if (response.isSuccessful()) {
                        AuthResponse data = response.body();
                        if (data.getUser() == null) {
                            String authCode = data.getAuthCode();
                            Log.i(SignInActivity.class.getSimpleName(), authCode);
                            Intent signupIntent = new Intent(context, SignUpActivity.class);
                            signupIntent.putExtra("email", data.getAccount());
                            signupIntent.putExtra("authCode", data.getAuthCode());
                            context.startActivityForResult(signupIntent, RC_SIGN_UP);
                        } else {
                            successCallback.response(data.getUser().getName(), data.getUser().getHash());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                    Toast.makeText(App.getInstance(), "Google error", Toast.LENGTH_LONG).show();
                }
            });
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(SignInActivity.class.getSimpleName(), "signInResult:failed code=" + e.getStatusCode());
        }
    }
}
