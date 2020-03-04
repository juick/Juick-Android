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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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
import com.juick.api.model.SecureUser;
import com.juick.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class SignInActivity extends AccountAuthenticatorActivity {

    public static final String EXTRA_ACTION = "EXTRA_ACTION";

    public static final int ACTION_PASSWORD_UPDATE = 1;

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SIGN_UP = 9002;

    private GoogleSignInClient googleClient;

    private int currentAction;

    private ActivityLoginBinding model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(model.getRoot());

        model.buttonSave.setOnClickListener(v -> {
            final String nick = model.juickNick.getText().toString();
            final String password = model.juickPassword.getText().toString();

            if (nick.length() == 0 || password.length() == 0) {
                Toast.makeText(SignInActivity.this, R.string.Enter_nick_and_password, Toast.LENGTH_SHORT).show();
                return;
            }

            App.getInstance().auth(nick, password, new Callback<SecureUser>() {
                @Override
                public void onResponse(Call<SecureUser> call, Response<SecureUser> response) {
                    if (response.isSuccessful() && response.code() == 200) {
                        updateAccount(nick, response.body().getHash(), currentAction);
                    } else {
                        Toast.makeText(App.getInstance(), R.string.Unknown_nick_or_wrong_password, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<SecureUser> call, Throwable t) {
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
        });

        // Button listeners
        model.signInButton.setOnClickListener(v -> {
            Intent signInIntent = googleClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getResources().getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        googleClient = GoogleSignIn.getClient(this, gso);

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = model.signInButton;
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);

        currentAction = getIntent().getIntExtra(EXTRA_ACTION, 0);

        if (Utils.hasAuth() && currentAction != ACTION_PASSWORD_UPDATE) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setNeutralButton(android.R.string.ok, (arg0, arg1) -> {
                setResult(RESULT_CANCELED);
                SignInActivity.this.finish();
            });
            builder.setMessage(R.string.Only_one_account);
            builder.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (requestCode == RC_SIGN_UP && data != null) {
            String nick = data.getStringExtra("nick");
            String password = data.getStringExtra("password");
            if (!TextUtils.isEmpty(nick)) {
                model.juickNick.setText(nick);
                model.juickPassword.setText(password);
                model.buttonSave.performClick();
            }
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            Log.i(SignInActivity.class.getSimpleName(), "Success " + account.getIdToken());
            App.getInstance().getApi().googleAuth(account.getIdToken()).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    if (response.isSuccessful()) {
                        AuthResponse data = response.body();
                        if (data.getAuthCode() != null) {
                            Log.i(SignInActivity.class.getSimpleName(), response.body().getAuthCode());
                            Intent signupIntent = new Intent(SignInActivity.this, SignUpActivity.class);
                            signupIntent.putExtra("email", response.body().getAccount());
                            signupIntent.putExtra("authCode", response.body().getAuthCode());
                            startActivityForResult(signupIntent, RC_SIGN_UP);
                        } else {
                            updateAccount(data.getUser().getName(), data.getUser().getHash(), 0);
                        }
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    Toast.makeText(App.getInstance(), "Google error", Toast.LENGTH_LONG).show();
                }
            });
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(SignInActivity.class.getSimpleName(), "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void updateAccount(String nick, String hash, int action) {
        Account account = new Account(nick, getString(R.string.com_juick));
        AccountManager am = AccountManager.get(SignInActivity.this);
        if (action == ACTION_PASSWORD_UPDATE) {
            am.setAuthToken(account, "", hash);
        } else {
            Bundle userData = new Bundle();
            userData.putString("hash", hash);
            am.addAccountExplicitly(account, "", userData);
        }
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, nick);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.com_juick));
        result.putString(AccountManager.KEY_AUTHTOKEN, hash);
        setAccountAuthenticatorResult(result);
        setResult(RESULT_OK);
        finish();
    }
}
