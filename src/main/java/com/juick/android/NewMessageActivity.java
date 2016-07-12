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

import android.Manifest;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.juick.App;
import com.juick.R;
import com.juick.remote.api.RestClient;
import com.juick.util.FileUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessageActivity extends BaseActivity implements OnClickListener {

    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;

    EditText etMessage;
    ImageView bTags;
    ImageView bAttachment;
    ImageView bSend;
    double lat = 0;
    double lon = 0;
    String attachmentUri = null;
    String attachmentMime = null;
    ProgressDialog progressDialog = null;
    BooleanReference progressDialogCancel = new BooleanReference(false);
    Handler progressHandler = new Handler() {

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
        setContentView(R.layout.activity_new_post);

        etMessage = (EditText) findViewById(R.id.editMessage);
        bTags = (ImageView) findViewById(R.id.buttonTags);
        bAttachment = (ImageView) findViewById(R.id.buttonAttachment);
        bSend = (ImageView) findViewById(R.id.buttonSend);

        bTags.setOnClickListener(this);
        bAttachment.setOnClickListener(this);
        bSend.setOnClickListener(this);

        resetForm();
        handleIntent(getIntent());
    }

    private void resetForm() {
        etMessage.setText("");
        bAttachment.setSelected(false);
        lat = 0;
        lon = 0;
        attachmentUri = null;
        attachmentMime = null;
        progressDialog = null;
        progressDialogCancel.bool = false;
        etMessage.requestFocus();
        //setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
    }

    private void setFormEnabled(boolean state) {
        etMessage.setEnabled(state);
        bTags.setEnabled(state);
        bAttachment.setEnabled(state);
        //setSupportProgressBarIndeterminateVisibility(state ? Boolean.FALSE : Boolean.TRUE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resetForm();
        handleIntent(intent);
    }

    private void handleIntent(Intent i) {
        String action = i.getAction();
        if (action != null && action.equals(Intent.ACTION_SEND)) {
            String mime = i.getType();
            Bundle extras = i.getExtras();
            if (mime.equals("text/plain")) {
                etMessage.append(extras.getString(Intent.EXTRA_TEXT));
            } else if (mime.equals("image/jpeg") || mime.equals("image/png")) {
                attachmentUri = extras.get(Intent.EXTRA_STREAM).toString();
                attachmentMime = mime;
                bAttachment.setSelected(true);
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSend:
                sendMessage();
                break;
            case R.id.buttonTags:
                replaceFragment(TagsFragment.newInstance(-1));
                break;
            case R.id.buttonAttachment:
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                    return;
                }
                if (attachmentUri == null) {
                    try {
                        Intent videoPickerIntent = new Intent();
                        videoPickerIntent.setType("video/*");
                        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                        videoPickerIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));

                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        Intent chooserIntent = Intent.createChooser(photoPickerIntent, null);
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{videoPickerIntent});

                        startActivityForResult(chooserIntent, ACTIVITY_ATTACHMENT_IMAGE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    attachmentUri = null;
                    attachmentMime = null;
                    bAttachment.setSelected(false);
                }
                break;
        }
    }

    private void sendMessage() {
        final String msg = etMessage.getText().toString();
        if (msg.length() < 3) {
            Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }
        setFormEnabled(false);
        if (attachmentUri != null) {
            progressDialog = new ProgressDialog(this);
            progressDialogCancel.bool = false;
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    NewMessageActivity.this.progressDialogCancel.bool = true;
                }
            });
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(0);
            progressDialog.show();
        }
        Thread thr = new Thread(new Runnable() {

            public void run() {
                final boolean res = sendMessage(NewMessageActivity.this, msg, lat, lon, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel);
                NewMessageActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        setFormEnabled(true);
                        if (res) {
                            resetForm();
                        }
                        if ((res && attachmentUri == null) || NewMessageActivity.this.isFinishing()) {
                            Toast.makeText(NewMessageActivity.this, res ? R.string.Message_posted : R.string.Error, Toast.LENGTH_LONG).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewMessageActivity.this);
                            builder.setNeutralButton(android.R.string.ok, null);
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

    public static boolean sendMessage(Context context, String txt, double lat, double lon, String attachmentUri, String attachmentMime, final ProgressDialog progressDialog, final Handler progressHandler, BooleanReference progressDialogCancel) {
        Log.e("sendMessage",attachmentMime+ " "+FileUtils.getPath(Uri.parse(attachmentUri)));
        try {
            File file = new File(FileUtils.getPath(Uri.parse(attachmentUri)));
            RequestBody requestFile =
                    RequestBody.create(MediaType.parse("multipart/form-data"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("attach", file.getName(), requestFile);

            return RestClient.getApi().newPost(RequestBody.create(MediaType.parse("text/plain"), txt),
                    RequestBody.create(MediaType.parse("text/plain"), String.valueOf(lat)),
                    RequestBody.create(MediaType.parse("text/plain"), String.valueOf(lon)),
                    body
                    /*new ProgressRequestBody(attachmentUri == null ? null : new File(FileUtils.getPath(Uri.parse(attachmentUri))), "png",
                    new ProgressRequestBody.UploadCallbacks() {

                    @Override
                    public void onProgressUpdate(int percentage) {

                    }

                    @Override
                    public void onError() {

                    }

                    @Override
                    public void onFinish() {

                    }
                })*/).execute().isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*try {
            final String end = "\r\n";
            final String twoHyphens = "--";
            final String boundary = "****+++++******+++++++********";

            URL apiUrl = new URL("https://api.juick.com/post");
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Authorization", AccountManager.getBasicAuthString());
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            String outStr = twoHyphens + boundary + end;
            outStr += "Content-Disposition: form-data; name=\"body\"" + end + end + txt + end;

            if (lat != 0 && lon != 0) {
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"lat\"" + end + end + String.valueOf(lat) + end;
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"lon\"" + end + end + String.valueOf(lon) + end;
            }

            if (attachmentUri != null && attachmentUri.length() > 0 && attachmentMime != null) {
                String fname = "file.";
                if (attachmentMime.equals("image/jpeg")) {
                    fname += "jpg";
                } else if (attachmentMime.equals("image/png")) {
                    fname += "png";
                }
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"attach\"; filename=\"" + fname + "\"" + end + end;
            }
            byte outStrB[] = outStr.getBytes("utf-8");

            String outStrEnd = twoHyphens + boundary + twoHyphens + end;
            byte outStrEndB[] = outStrEnd.getBytes();

            int size = outStrB.length + outStrEndB.length;

            FileInputStream fileInput = null;
            if (attachmentUri != null && attachmentUri.length() > 0) {
                fileInput = context.getContentResolver().openAssetFileDescriptor(Uri.parse(attachmentUri), "r").createInputStream();
                size += fileInput.available();
                size += 2; // \r\n (end)
            }

            if (progressDialog != null) {
                progressHandler.sendEmptyMessage(size);
            }

            conn.setFixedLengthStreamingMode(size);
            conn.connect();
            OutputStream out = conn.getOutputStream();
            out.write(outStrB);

            if (attachmentUri != null && attachmentUri.length() > 0 && fileInput != null) {
                byte[] buffer = new byte[4096];
                int length = -1;
                int total = 0;
                int totallast = 0;
                while ((length = fileInput.read(buffer, 0, 4096)) != -1 && !progressDialogCancel.bool) {
                    out.write(buffer, 0, length);
                    total += length;
                    if (((int) (total / 102400)) != totallast) {
                        totallast = (int) (total / 102400);
                        progressHandler.sendEmptyMessage(total);
                    }
                }
                if (!progressDialogCancel.bool) {
                    out.write(end.getBytes());
                }
                fileInput.close();
                progressHandler.sendEmptyMessage(size);
            }
            if (!progressDialogCancel.bool) {
                out.write(outStrEndB);
                out.flush();
            }
            out.close();

            if (progressDialogCancel.bool) {
                return false;
            } else {
                return (conn.getResponseCode() == 200);
            }
        } catch (Exception e) {
            Log.e("sendOpinion", e.toString());
        }*/
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = data.getDataString();
                // How to get correct mime type?
                attachmentMime = "image/jpeg";
                bAttachment.setSelected(true);
            }
        }
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TagsFragment.TAG_SELECT_ACTION)) {
                etMessage.setText("*" + intent.getStringExtra(TagsFragment.ARG_TAG) + " " + etMessage.getText());
            } else if (intent.getAction().equals(RestClient.ACTION_UPLOAD_PROGRESS)) {
                if (progressDialog != null) {
                    progressHandler.sendEmptyMessage(intent.getIntExtra(RestClient.EXTRA_PROGRESS, 0));
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(TagsFragment.TAG_SELECT_ACTION));
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(broadcastReceiver, new IntentFilter(RestClient.ACTION_UPLOAD_PROGRESS));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(broadcastReceiver);
    }

    public static class BooleanReference {

        public boolean bool;

        public BooleanReference(boolean bool) {
            this.bool = bool;
        }
    }

    @Override
    public int fragmentContainerLayoutId() {
        return 0;
    }

    @Override
    public int getTabsBarLayoutId() {
        return 0;
    }
}
