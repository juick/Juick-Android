/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.R;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author ugnich
 */
public class SignInActivity extends Activity implements OnClickListener {

    private EditText etNick;
    private EditText etPassword;
    private Button bSave;
    private Button bCancel;
    private Handler handlErrToast = new Handler() {

        public void handleMessage(Message msg) {
            Toast.makeText(SignInActivity.this, R.string.Unknown_nick_or_wrong_password, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.signin);

        etNick = (EditText) findViewById(R.id.juickNick);
        etPassword = (EditText) findViewById(R.id.juickPassword);
        bSave = (Button) findViewById(R.id.buttonSave);
        bCancel = (Button) findViewById(R.id.buttonCancel);

        bSave.setOnClickListener(this);
        bCancel.setOnClickListener(this);

        if (Utils.hasAuth(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setNeutralButton(R.string.OK, new android.content.DialogInterface.OnClickListener() {

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
        if (view == bCancel) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        final String nick = etNick.getText().toString();
        final String password = etPassword.getText().toString();

        if (nick.length() == 0 || password.length() == 0) {
            Toast.makeText(this, R.string.Enter_nick_and_password, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

        Thread thr = new Thread(new Runnable() {

            public void run() {
                int status = 0;
                try {
                    String authStr = nick + ":" + password;
                    String basicAuth = "Basic " + Base64.encodeToString(authStr.getBytes(), Base64.NO_WRAP);

                    URL apiUrl = new URL("http://api.juick.com/post");
                    HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", basicAuth);
                    conn.connect();
                    status = conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e("checkingNickPassw", e.toString());
                }
                if (status == 400) {
                    Account account = new Account(nick, getString(R.string.com_juick));
                    AccountManager am = AccountManager.get(SignInActivity.this);
                    boolean accountCreated = am.addAccountExplicitly(account, password, null);
                    Bundle extras = getIntent().getExtras();
                    if (extras != null && accountCreated) {
                        AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                        Bundle result = new Bundle();
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, nick);
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.com_juick));
                        response.onResult(result);
                    }

                    SignInActivity.this.setResult(RESULT_OK);
                    SignInActivity.this.finish();
                } else {
                    handlErrToast.sendEmptyMessage(0);
                }
            }
        });
        thr.start();
    }
}
