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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.juick.R;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessageActivity extends AppCompatActivity implements OnClickListener {

    private static final int ACTIVITY_LOCATION = 1;
    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;
    private static final int ACTIVITY_TAGS = 4;
    private EditText etMessage;
    private ImageButton bTags;
    private ImageButton bLocation;
    private ImageButton bAttachment;
    private double lat = 0;
    private double lon = 0;
    private String attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog = null;
    private BooleanReference progressDialogCancel = new BooleanReference(false);
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

//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        setTitle(R.string.New_message);

        setContentView(R.layout.newmessage);

        etMessage = (EditText) findViewById(R.id.editMessage);
        bTags = (ImageButton) findViewById(R.id.buttonTags);
        bLocation = (ImageButton) findViewById(R.id.buttonLocation);
        bAttachment = (ImageButton) findViewById(R.id.buttonAttachment);

        bTags.setOnClickListener(this);
        bLocation.setOnClickListener(this);
        bAttachment.setOnClickListener(this);

        resetForm();
        handleIntent(getIntent());
    }

    private void resetForm() {
        etMessage.setText("");
        bLocation.setSelected(false);
        bAttachment.setSelected(false);
        lat = 0;
        lon = 0;
        attachmentUri = null;
        attachmentMime = null;
        progressDialog = null;
        progressDialogCancel.bool = false;
        etMessage.requestFocus();
        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

        /*
        setProgressBarIndeterminateVisibility(true);
        Thread thr = new Thread(new Runnable() {
        
        public void run() {
        String jsonUrl = "http://api.juick.com/postform";
        
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (loc != null) {
        jsonUrl += "?lat=" + loc.getLatitude() + "&lon=" + loc.getLongitude() + "&acc=" + loc.getAccuracy() + "&fixage=" + Math.round((System.currentTimeMillis() - loc.getTime()) / 1000);
        }
        
        final String jsonStr = Utils.getJSON(NewMessageActivity.this, jsonUrl);
        
        NewMessageActivity.this.runOnUiThread(new Runnable() {
        
        public void run() {
        if (jsonStr != null) {
        
        try {
        JSONObject json = new JSONObject(jsonStr);
        if (json.has("facebook")) {
        etTo.setText(etTo.getText() + ", Facebook");
        }
        if (json.has("twitter")) {
        etTo.setText(etTo.getText() + ", Twitter");
        }
        if (json.has("place")) {
        JSONObject jsonPlace = json.getJSONObject("place");
        pidHint = jsonPlace.getInt("pid");
        bLocationHint.setVisibility(View.VISIBLE);
        bLocationHint.setText(jsonPlace.getString("name"));
        }
        } catch (JSONException e) {
        System.err.println(e);
        }
        }
        NewMessageActivity.this.setProgressBarIndeterminateVisibility(false);
        }
        });
        }
        });
        thr.start();
         */
    }

    private void setFormEnabled(boolean state) {
        etMessage.setEnabled(state);
        bTags.setEnabled(state);
        bLocation.setEnabled(state);
        bAttachment.setEnabled(state);
        setSupportProgressBarIndeterminateVisibility(state ? Boolean.FALSE : Boolean.TRUE);
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
        if (v == bTags) {
            Intent i = new Intent(this, TagsActivity.class);
            i.setAction(Intent.ACTION_PICK);
            i.putExtra("uid", (int) -1);
            startActivityForResult(i, ACTIVITY_TAGS);

        } else if (v == bLocation) {
            if (lat == 0 && lon == 0) {
                startActivityForResult(new Intent(this, PickLocationActivity.class), ACTIVITY_LOCATION);
            } else {
                lat = 0;
                lon = 0;
                bLocation.setSelected(false);
            }
        } else if (v == bAttachment) {
            if (attachmentUri == null) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_IMAGE);
            } else {
                attachmentUri = null;
                attachmentMime = null;
                bAttachment.setSelected(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.newmessage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuitem_send) {
            final String msg = etMessage.getText().toString();
            if (msg.length() < 3) {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
                return false;
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
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public static boolean sendMessage(Context context, String txt, double lat, double lon, String attachmentUri, String attachmentMime, final ProgressDialog progressDialog, Handler progressHandler, BooleanReference progressDialogCancel) {
        try {
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
            conn.setRequestProperty("Authorization", Utils.getBasicAuthString(context));
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
                final int fsize = size;
                progressHandler.sendEmptyMessage(fsize);
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
                while ((length = fileInput.read(buffer, 0, 4096)) != -1 && progressDialogCancel.bool == false) {
                    out.write(buffer, 0, length);
                    total += length;
                    if (((int) (total / 102400)) != totallast) {
                        totallast = (int) (total / 102400);
                        progressHandler.sendEmptyMessage(total);
                    }
                }
                if (progressDialogCancel.bool == false) {
                    out.write(end.getBytes());
                }
                fileInput.close();
                progressHandler.sendEmptyMessage(size);
            }
            if (progressDialogCancel.bool == false) {
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
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTIVITY_TAGS) {
                etMessage.setText("*" + data.getStringExtra("tag") + " " + etMessage.getText());
            } else if (requestCode == ACTIVITY_LOCATION) {
                lat = data.getDoubleExtra("lat", 0);
                lon = data.getDoubleExtra("lon", 0);
                if (lat != 0 && lon != 0) {
                    bLocation.setSelected(true);
                }
            } else if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = data.getDataString();
                // How to get correct mime type?
                attachmentMime = "image/jpeg";
                bAttachment.setSelected(true);
            }
        }
    }

    public static class BooleanReference {

        public boolean bool;

        public BooleanReference(boolean bool) {
            this.bool = bool;
        }
    }
}
