package com.juick.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.R;
import com.juick.api.RestClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    private String email;
    private String authCode;

    private EditText newNick, newPassword, confirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        email = getIntent().getStringExtra("email");
        authCode = getIntent().getStringExtra("authCode");
        setContentView(R.layout.signup);
        newNick = findViewById(R.id.newNick);
        newPassword = findViewById(R.id.newPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        Button accountCreateButton = findViewById(R.id.buttonCreate);
        accountCreateButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String nick = newNick.getText().toString();
        String password = newPassword.getText().toString();
        String confirm = confirmPassword.getText().toString();
        if (!password.equals(confirm)) {
            Toast.makeText(this,"Passwords did not match", Toast.LENGTH_LONG).show();
            return;
        }
        RestClient.getApi().signup(nick, password, authCode).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
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
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }
}
