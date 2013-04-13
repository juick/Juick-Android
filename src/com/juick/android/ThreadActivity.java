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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.juick.R;
import java.net.URLEncoder;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadActivity extends FragmentActivity implements View.OnClickListener, DialogInterface.OnClickListener, ThreadFragment.ThreadFragmentListener {

    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;
    public static final int ACTIVITY_ATTACHMENT_VIDEO = 3;
    private TextView tvReplyTo;
    private EditText etMessage;
    private Button bSend;
    private ImageButton bAttach;
    private int mid = 0;
    private int rid = 0;
    private String attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog = null;
    private NewMessageActivity.BooleanReference progressDialogCancel = new NewMessageActivity.BooleanReference(false);
    private Handler progressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (progressDialog.getMax() < msg.what) {
                progressDialog.setMax(msg.what);
            } else {
                progressDialog.setProgress(msg.what);
            }
        }
    };

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
        bAttach = (ImageButton) findViewById(R.id.buttonAttachment);
        bAttach.setOnClickListener(this);

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
        attachmentMime = null;
        attachmentUri = null;
        bAttach.setSelected(false);
        setFormEnabled(true);
    }

    private void setFormEnabled(boolean state) {
        etMessage.setEnabled(state);
        bSend.setEnabled(state);
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
        if (view == bAttach) {
            if (attachmentUri == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.Attach);
                builder.setAdapter(new AttachAdapter(this), this);
                builder.show();
            } else {
                attachmentUri = null;
                attachmentMime = null;
                bAttach.setSelected(false);
            }
        } else if (view == bSend) {
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

            setFormEnabled(false);

            if (attachmentUri == null) {
                postText(body);
            } else {
                postMedia(body);
            }
        }
    }

    public void postText(final String body) {
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
                                setFormEnabled(true);
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

    public void postMedia(final String body) {
        progressDialog = new ProgressDialog(this);
        progressDialogCancel.bool = false;
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                ThreadActivity.this.progressDialogCancel.bool = true;
            }
        });
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(0);
        progressDialog.show();
        Thread thr = new Thread(new Runnable() {

            public void run() {
                final boolean res = NewMessageActivity.sendMessage(ThreadActivity.this, body, 0, 0, 0, 0, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel);
                ThreadActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        setFormEnabled(true);
                        if (res) {
                            resetForm();
                        }
                        if (res && attachmentUri == null) {
                            Toast.makeText(ThreadActivity.this, R.string.Message_posted, Toast.LENGTH_LONG).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(ThreadActivity.this);
                            builder.setNeutralButton(R.string.OK, null);
                            if (res) {
                                builder.setIcon(android.R.drawable.ic_dialog_info);
                                builder.setMessage(R.string.Message_posted);
                            } else {
                                builder.setIcon(android.R.drawable.ic_dialog_alert);
                                builder.setMessage(R.string.Error);
                            }
                            builder.show();
                        }
                    }
                });
            }
        });
        thr.start();
    }

    public void onClick(DialogInterface dialog, int which) {
        Intent intent;
        switch (which) {
            case 0:
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_IMAGE);
                break;
            case 1:
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
                startActivityForResult(intent, ACTIVITY_ATTACHMENT_IMAGE);
                break;
            case 2:
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_VIDEO);
                break;
            case 3:
                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(intent, ACTIVITY_ATTACHMENT_VIDEO);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if ((requestCode == ACTIVITY_ATTACHMENT_IMAGE || requestCode == ACTIVITY_ATTACHMENT_VIDEO) && data != null) {
                attachmentUri = data.getDataString();
                attachmentMime = (requestCode == ACTIVITY_ATTACHMENT_IMAGE) ? "image/jpeg" : "video/3gpp";
                bAttach.setSelected(true);
            }
        }
    }
}
