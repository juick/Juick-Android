/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.juick.R;
import java.net.URLEncoder;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class SignUpActivity extends Activity implements View.OnClickListener {

    private EditText etNick;
    private EditText etPassword;
    private EditText etCaptcha;
    private ImageView imgCaptcha;
    private Button bCreate;
    private Button bCancel;
    private static String captchaID = "";
    private Handler handlErrToast = new Handler() {

        public void handleMessage(Message msg) {
            setProgressBarIndeterminateVisibility(false);
            Toast.makeText(SignUpActivity.this, msg.getData().getString("message"), Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.updateTheme(this);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.signup);

        etNick = (EditText) findViewById(R.id.juickNick);
        etPassword = (EditText) findViewById(R.id.juickPassword);
        etCaptcha = (EditText) findViewById(R.id.juickCaptcha);
        imgCaptcha = (ImageView) findViewById(R.id.imgCaptcha);
        bCreate = (Button) findViewById(R.id.buttonCreate);
        bCancel = (Button) findViewById(R.id.buttonCancel);

        bCreate.setOnClickListener(this);
        bCancel.setOnClickListener(this);

        setProgressBarIndeterminateVisibility(true);
        getCaptcha();
    }

    private void getCaptcha() {
        Thread thr = new Thread(new Runnable() {

            public void run() {
                String jsonStr = Utils.getJSON(SignUpActivity.this, "http://api.juick.com/captcha");
                if (jsonStr != null) {
                    try {
                        JSONObject json = new JSONObject(jsonStr);
                        final String imgURL = json.getString("image");
                        if (imgURL != null && imgURL.length() > 0) {
                            imgCaptcha.post(new Runnable() {

                                public void run() {
                                    etCaptcha.setText("");
                                    imgCaptcha.setImageBitmap(Utils.downloadImage(imgURL));
                                    SignUpActivity.this.setProgressBarIndeterminateVisibility(false);
                                }
                            });
                            captchaID = json.getString("id");
                        }
                    } catch (Exception e) {
                        Log.e("gettingCaptcha", e.toString());
                    }
                }
            }
        });
        thr.start();
    }

    public void onClick(View view) {
        if (view == bCancel) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        final String nick = etNick.getText().toString();
        final String password = etPassword.getText().toString();
        final String captcha = etCaptcha.getText().toString();

        if (nick.length() == 0 || password.length() == 0 || captcha.length() == 0 || !nick.matches("[a-zA-Z0-9\\-]+")) {
            Toast.makeText(this, R.string.Enter_nick_and_password, Toast.LENGTH_SHORT).show();
            return;
        }

        setProgressBarIndeterminateVisibility(true);
        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

        Thread thr = new Thread(new Runnable() {

            public void run() {
                try {
                    String qs = "http://api.juick.com/signup?";
                    qs += "captcha_id=" + captchaID + "&captcha_text=" + URLEncoder.encode(captcha, "utf-8");
                    qs += "&nick=" + nick + "&password=" + URLEncoder.encode(password, "utf-8");
                    int status = Utils.doHttpGetRequest(qs);
                    if (status == 200) {
                        SharedPreferences.Editor settingsEditor = PreferenceManager.getDefaultSharedPreferences(SignUpActivity.this).edit();
                        settingsEditor.putString("nick", nick);
                        settingsEditor.putString("password", password);
                        settingsEditor.commit();
                        SignUpActivity.this.setResult(RESULT_OK);
                        SignUpActivity.this.finish();
                    } else {
                        String errStr;
                        if (status == 400) {
                            errStr = getResources().getString(R.string.Wrong_captcha);
                        } else if (status == 403) {
                            getCaptcha();
                            errStr = getResources().getString(R.string.Nick_is_already_exists);
                        } else {
                            errStr = getResources().getString(R.string.Error);
                        }
                        Message msg = Message.obtain();
                        Bundle b = new Bundle();
                        b.putString("message", errStr);
                        msg.setData(b);
                        handlErrToast.sendMessage(msg);
                    }
                } catch (Exception e) {
                    Log.e("signup", e.toString());
                }
            }
        });
        thr.start();
    }
}
