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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.juick.R;
import java.net.URLEncoder;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadActivity extends FragmentActivity implements View.OnClickListener, ThreadFragment.ThreadFragmentListener {

    private TextView tvReplyTo;
    private EditText etMessage;
    private Button bSend;
    private int mid = 0;
    private int rid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        mid = i.getIntExtra("mid", 0);
        if (mid == 0) {
            finish();
        }

        setContentView(R.layout.thread);
        tvReplyTo = (TextView) findViewById(R.id.textReplyTo);
        etMessage = (EditText) findViewById(R.id.editMessage);
        bSend = (Button) findViewById(R.id.buttonSend);
        bSend.setOnClickListener(this);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ThreadFragment tf = new ThreadFragment();
        Bundle args = new Bundle();
        args.putInt("mid", mid);
        tf.setArguments(args);
        ft.add(R.id.threadfragment, tf);
        ft.commit();
    }

    private void resetForm() {
        rid = 0;
        tvReplyTo.setVisibility(View.GONE);
        etMessage.setText("");
        etMessage.setEnabled(true);
        bSend.setEnabled(true);
    }

    public void onThreadLoaded(int uid, String nick) {
        setTitle("@" + nick);
    }

    public void onReplySelected(int newrid, String txt) {
        rid = newrid;
        if (rid > 0) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            String inreplyto = getResources().getString(R.string.In_reply_to_) + " ";
            ssb.append(inreplyto + txt);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, inreplyto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvReplyTo.setText(ssb);
            tvReplyTo.setVisibility(View.VISIBLE);
        } else {
            tvReplyTo.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        String msg = etMessage.getText().toString();
        if (msg.length() < 3) {
            Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }
//        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

        String msgnum = "#" + mid;
        if (rid > 0) {
            msgnum += "/" + rid;
        }
        final String body = msgnum + " " + msg;

        etMessage.setEnabled(false);
        bSend.setEnabled(false);

        Thread thr = new Thread(new Runnable() {

            public void run() {
                try {
                    final String ret = Utils.postJSON(ThreadActivity.this, "http://api.juick.com/post", "body=" + URLEncoder.encode(body, "utf-8"));
                    ThreadActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            if (ret != null) {
                                Toast.makeText(ThreadActivity.this, R.string.Message_posted, Toast.LENGTH_SHORT).show();
                                resetForm();
                            } else {
                                Toast.makeText(ThreadActivity.this, R.string.Error, Toast.LENGTH_SHORT).show();
                                etMessage.setEnabled(true);
                                bSend.setEnabled(true);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("postComment", e.toString());
                }
            }
        });
        thr.start();
    }
}
