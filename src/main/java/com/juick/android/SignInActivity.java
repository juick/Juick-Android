/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.App;
import com.juick.R;
import com.juick.api.RestClient;
import com.juick.android.service.RegistrationIntentService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class SignInActivity extends Activity implements OnClickListener {

    private EditText etNick;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNick = (EditText) findViewById(R.id.juickNick);
        etPassword = (EditText) findViewById(R.id.juickPassword);
        findViewById(R.id.buttonSave).setOnClickListener(this);

        if (Utils.hasAuth()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface arg0, int arg1) {
                    setResult(RESULT_CANCELED);
                    SignInActivity.this.finish();
                }
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

                RestClient.auth(nick, password, new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (response.code() == 400) {
                            Account account = new Account(nick, getString(R.string.com_juick));
                            AccountManager am = AccountManager.get(SignInActivity.this);
                            boolean accountCreated = am.addAccountExplicitly(account, password, null);
                            Bundle extras = getIntent().getExtras();
                            if (extras != null && accountCreated) {
                                AccountAuthenticatorResponse accountAuthenticatorResponse = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                                Bundle result = new Bundle();
                                result.putString(AccountManager.KEY_ACCOUNT_NAME, nick);
                                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.com_juick));
                                accountAuthenticatorResponse.onResult(result);
                            }

                            startService(new Intent(SignInActivity.this, RegistrationIntentService.class));
                            SignInActivity.this.setResult(RESULT_OK);
                            SignInActivity.this.finish();
                        } else {
                            Toast.makeText(App.getInstance(), R.string.Unknown_nick_or_wrong_password, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                    }
                });
                break;
        }
    }
}
