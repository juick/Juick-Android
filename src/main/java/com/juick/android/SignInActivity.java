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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.juick.App;
import com.juick.R;
import com.juick.api.model.SecureUser;
import com.juick.databinding.ActivityLoginBinding;
import com.juick.util.StringUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class SignInActivity extends AppCompatActivity {

    public enum SignInStatus {
        SIGNED_OUT, SIGN_IN_PROGRESS, SIGNED_IN
    }

    private AccountAuthenticatorResponse authenticatorResponse;

    public static final String EXTRA_ACTION = "EXTRA_ACTION";

    public static final int ACTION_ACCOUNT_CREATE = 0;
    public static final int ACTION_PASSWORD_UPDATE = 1;

    private int currentAction;

    private ActivityLoginBinding model;

    private final App application = App.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (authenticatorResponse != null) {
            authenticatorResponse.onRequestContinued();
        }

        model = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(model.getRoot());

        model.buttonSave.setOnClickListener(v -> {
            final String nick = model.juickNick.getText().toString();
            final String password = model.juickPassword.getText().toString();

            if (nick.length() == 0 || password.length() == 0) {
                Toast.makeText(SignInActivity.this, R.string.Enter_nick_and_password, Toast.LENGTH_SHORT).show();
                return;
            }

            application.auth(nick, password, new Callback<SecureUser>() {
                @Override
                public void onResponse(@NonNull Call<SecureUser> call, @NonNull Response<SecureUser> response) {
                    if (response.isSuccessful() && response.code() == 200 && response.body() != null) {
                        updateAccount(nick, response.body().getHash(), currentAction);
                    } else {
                        Toast.makeText(SignInActivity.this, R.string.Unknown_nick_or_wrong_password, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SecureUser> call, @NonNull Throwable t) {
                    CharSequence errorMessage = getText(R.string.network_error);
                    Toast.makeText(SignInActivity.this, String.format("%s: %s", errorMessage,
                            StringUtils.defaultString(t.getMessage())), Toast.LENGTH_LONG).show();
                }
            });
        });
        FrameLayout signInButton = (FrameLayout) application.getSignInProvider()
                .prepareSignIn(this, model.signInButtonPlaceholder);
        if (signInButton != null) {
            // Button listeners
            signInButton.setOnClickListener(v -> application.getSignInProvider().performSignIn());
        }

        currentAction = getIntent().getIntExtra(EXTRA_ACTION, ACTION_ACCOUNT_CREATE);

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
        application.getSignInProvider().onSignInResult(requestCode, resultCode, data, (nick, password) -> {
            if (!TextUtils.isEmpty(nick)) {
                model.juickNick.setText(nick);
                model.juickPassword.setText(password);
                model.buttonSave.performClick();
            }
        }, (username, hash) -> updateAccount(username, hash, ACTION_ACCOUNT_CREATE));
    }

    private void updateAccount(String nick, String hash, int action) {
        Account account = new Account(nick, getString(R.string.com_juick));
        AccountManager am = AccountManager.get(SignInActivity.this);
        if (action == ACTION_PASSWORD_UPDATE) {
            am.setAuthToken(account, StringUtils.EMPTY, hash);
        } else {
            Bundle userData = new Bundle();
            userData.putString("hash", hash);
            am.addAccountExplicitly(account, StringUtils.EMPTY, userData);
        }
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, nick);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.com_juick));
        result.putString(AccountManager.KEY_AUTHTOKEN, hash);
        if (authenticatorResponse != null) {
            authenticatorResponse.onResult(result);
        }
        setResult(RESULT_OK);
        finish();
    }
}
