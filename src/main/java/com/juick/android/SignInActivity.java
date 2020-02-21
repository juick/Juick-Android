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
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.juick.App;
import com.juick.R;
import com.juick.api.RestClient;
import com.juick.api.model.AuthToken;
import com.juick.api.model.SecureUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class SignInActivity extends AccountAuthenticatorActivity implements OnClickListener {

    public static final String EXTRA_ACTION = "EXTRA_ACTION";

    public static final int ACTION_PASSWORD_UPDATE = 1;

    private EditText etNick;
    private EditText etPassword;
    private Button loginButton;

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SIGN_UP = 9002;

    private GoogleSignInClient googleClient;

    private int currentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNick = findViewById(R.id.juickNick);
        etPassword = findViewById(R.id.juickPassword);
        loginButton = findViewById(R.id.buttonSave);
        loginButton.setOnClickListener(this);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getResources().getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleSignInClient with the options specified by gso.
        googleClient = GoogleSignIn.getClient(this, gso);
        // [END build_client]

        // [START customize_button]
        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
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

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonSave:
                final String nick = etNick.getText().toString();
                final String password = etPassword.getText().toString();

                if (nick.length() == 0 || password.length() == 0) {
                    Toast.makeText(this, R.string.Enter_nick_and_password, Toast.LENGTH_SHORT).show();
                    return;
                }

                RestClient.getInstance().auth(nick, password, new Callback<SecureUser>() {
                    @Override
                    public void onResponse(Call<SecureUser> call, Response<SecureUser> response) {
                        if (response.isSuccessful() && response.code() == 200) {
                            Account account = new Account(nick, getString(R.string.com_juick));
                            AccountManager am = AccountManager.get(SignInActivity.this);
                            String hash = response.body().getHash();
                            if (currentAction == ACTION_PASSWORD_UPDATE) {
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
                            SignInActivity.this.setResult(RESULT_OK);
                            SignInActivity.this.finish();
                        } else {
                            Toast.makeText(App.getInstance(), R.string.Unknown_nick_or_wrong_password, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SecureUser> call, Throwable t) {
                        Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = googleClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
                etNick.setText(nick);
                etPassword.setText(password);
                loginButton.performClick();
            }
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            Log.i(SignInActivity.class.getSimpleName(), "Success " + account.getIdToken());
            RestClient.getInstance().getApi().googleAuth(account.getIdToken()).enqueue(new Callback<AuthToken>() {
                @Override
                public void onResponse(Call<AuthToken> call, Response<AuthToken> response) {
                    if (response.isSuccessful()) {
                        Log.i(SignInActivity.class.getSimpleName(), response.body().getAuthCode());
                        Intent signupIntent = new Intent(SignInActivity.this, SignUpActivity.class);
                        signupIntent.putExtra("email", response.body().getAccount());
                        signupIntent.putExtra("authCode", response.body().getAuthCode());
                        startActivityForResult(signupIntent, RC_SIGN_UP);
                    } else {
                        Toast.makeText(App.getInstance(), "Email already registered", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<AuthToken> call, Throwable t) {

                }
            });
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(SignInActivity.class.getSimpleName(), "signInResult:failed code=" + e.getStatusCode());
        }
    }
}
