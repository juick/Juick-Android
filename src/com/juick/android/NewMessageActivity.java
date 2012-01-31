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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.juick.R;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessageActivity extends Activity implements OnClickListener, DialogInterface.OnClickListener {

    private static final int ACTIVITY_LOCATION = 1;
    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;
    public static final int ACTIVITY_ATTACHMENT_VIDEO = 3;
    private static final int ACTIVITY_TAGS = 4;
    private EditText etTo;
    private EditText etMessage;
    private Button bLocationHint;
    private ImageButton bTags;
    private ImageButton bLocation;
    private ImageButton bAttachment;
    private Button bSend;
    private ProgressBar progressSend;
    private int pid = 0;
    private int pidHint = 0;
    private String pname = null;
    private double lat = 0;
    private double lon = 0;
    private int acc = 0;
    private String attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog = null;
    private static boolean progressDialogCancel;
    private Handler progressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            progressDialog.setProgress(msg.what);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.updateTheme(this);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.newmessage);

        etTo = (EditText) findViewById(R.id.editTo);
        etMessage = (EditText) findViewById(R.id.editMessage);
        bLocationHint = (Button) findViewById(R.id.buttonLocationHint);
        bTags = (ImageButton) findViewById(R.id.buttonTags);
        bLocation = (ImageButton) findViewById(R.id.buttonLocation);
        bAttachment = (ImageButton) findViewById(R.id.buttonAttachment);
        bSend = (Button) findViewById(R.id.buttonSend);
        progressSend = (ProgressBar) findViewById(R.id.progressSend);

        bLocationHint.setOnClickListener(this);
        bTags.setOnClickListener(this);
        bLocation.setOnClickListener(this);
        bAttachment.setOnClickListener(this);
        bSend.setOnClickListener(this);

        resetForm();
        /*
        if (savedInstanceState!=null) {
        if (savedInstanceState.containsKey("msg")) {
        etMessage.setText(savedInstanceState.getString("msg"));
        }
        pid = savedInstanceState.getInt("pid", 0);
        pname = savedInstanceState.getString("pname");
        lat = savedInstanceState.getDouble("lat", 0);
        lon = savedInstanceState.getDouble("lon", 0);
        attachmentUri = savedInstanceState.getString("auri");
        attachmentMime = savedInstanceState.getString("amime");
        if (pid > 0 && pname != null) {
        tvLocation.setText(getResources().getString(R.string.label_location) + " " + pname);
        tvLocation.setVisibility(View.VISIBLE);
        }
        if (attachmentUri != null && attachmentMime != null) {
        tvAttachment.setText(attachmentMime.equals("image/jpeg") ? R.string.attachment_photo : R.string.attachment_video);
        tvAttachment.setVisibility(View.VISIBLE);
        }
        }
         */
        handleIntent(getIntent());
    }
    /*
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (etMessage.getText().toString().length() > 0) {
    outState.putString("msg", etMessage.getText().toString());
    }
    if (pid > 0 && pname != null) {
    outState.putInt("pid", pid);
    outState.putString("pname", tvLocation.getText().toString());
    }
    if (lat != 0 && lon != 0 && pname != null) {
    outState.putDouble("lat", lat);
    outState.putDouble("lon", lon);
    outState.putString("pname", tvLocation.getText().toString());
    }
    if (attachmentUri != null && attachmentMime != null) {
    outState.putString("auri", attachmentUri);
    outState.putString("amime", attachmentMime);
    }
    }
     */

    private void resetForm() {
        setProgressBarIndeterminateVisibility(true);
        etMessage.setText("");
        bLocationHint.setVisibility(View.GONE);
        bLocation.setSelected(false);
        bAttachment.setSelected(false);
        pid = 0;
        pidHint = 0;
        pname = null;
        lat = 0;
        lon = 0;
        acc = 0;
        attachmentUri = null;
        attachmentMime = null;
        progressDialog = null;
        progressDialogCancel = false;
        etMessage.requestFocus();

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
    }

    private void setFormEnabled(boolean state) {
        etMessage.setEnabled(state);
        bLocationHint.setEnabled(state);
        bTags.setEnabled(state);
        bLocation.setEnabled(state);
        bAttachment.setEnabled(state);
        bSend.setVisibility(state ? View.VISIBLE : View.GONE);
        progressSend.setVisibility(state ? View.GONE : View.VISIBLE);
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
            } else if (mime.equals("image/jpeg") || mime.equals("video/3gpp") || mime.equals("video/mp4")) {
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
        } else if (v == bLocationHint) {
            bLocationHint.setVisibility(View.GONE);
            pid = pidHint;
            pname = null;
            lat = 0;
            lon = 0;
            acc = 0;
            bLocation.setSelected(true);
        } else if (v == bLocation) {
            bLocationHint.setVisibility(View.GONE);
            if (pid == 0 && lat == 0) {
                startActivityForResult(new Intent(this, PickPlaceActivity.class), ACTIVITY_LOCATION);
            } else {
                pid = 0;
                pname = null;
                lat = 0;
                lon = 0;
                acc = 0;
                bLocation.setSelected(false);
            }
        } else if (v == bAttachment) {
            if (attachmentUri == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.Attach);
                builder.setAdapter(new AttachAdapter(this), this);
                builder.show();
            } else {
                attachmentUri = null;
                attachmentMime = null;
                bAttachment.setSelected(false);
            }
        } else if (v == bSend) {
            final String msg = etMessage.getText().toString();
            if (msg.length() < 3) {
                Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
                return;
            }
            setFormEnabled(false);
            if (attachmentUri != null) {
                progressDialog = new ProgressDialog(this);
                progressDialogCancel = false;
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    public void onCancel(DialogInterface arg0) {
                        NewMessageActivity.progressDialogCancel = true;
                    }
                });
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();
            }
            Thread thr = new Thread(new Runnable() {

                public void run() {
                    final boolean res = sendMessage(msg);
                    NewMessageActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            setFormEnabled(true);
                            if (res) {
                                resetForm();
                            }
                            if (res && attachmentUri == null) {
                                Toast.makeText(NewMessageActivity.this, R.string.Message_posted, Toast.LENGTH_LONG).show();
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
        }
    }

    private boolean sendMessage(String txt) {
        try {
            final String end = "\r\n";
            final String twoHyphens = "--";
            final String boundary = "****+++++******+++++++********";

            URL apiUrl = new URL("http://api.juick.com/post");
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Authorization", Utils.getBasicAuthString(this));
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            String outStr = twoHyphens + boundary + end;
            outStr += "Content-Disposition: form-data; name=\"body\"" + end + end + txt + end;

            if (pid > 0) {
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"place_id\"" + end + end + pid + end;
            }
            if (lat != 0 && lon != 0) {
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"lat\"" + end + end + String.valueOf(lat) + end;
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"lon\"" + end + end + String.valueOf(lon) + end;
                if (acc > 0) {
                    outStr += twoHyphens + boundary + end;
                    outStr += "Content-Disposition: form-data; name=\"acc\"" + end + end + String.valueOf(acc) + end;
                }
            }

            if (attachmentUri != null && attachmentUri.length() > 0 && attachmentMime != null) {
                String fname = "file." + (attachmentMime.equals("image/jpeg") ? "jpg" : "3gp");
                outStr += twoHyphens + boundary + end;
                outStr += "Content-Disposition: form-data; name=\"attach\"; filename=\"" + fname + "\"" + end + end;
            }
            byte outStrB[] = outStr.getBytes("utf-8");

            String outStrEnd = twoHyphens + boundary + twoHyphens + end;
            byte outStrEndB[] = outStrEnd.getBytes();

            int size = outStrB.length + outStrEndB.length;

            FileInputStream fileInput = null;
            if (attachmentUri != null && attachmentUri.length() > 0) {
                fileInput = getContentResolver().openAssetFileDescriptor(Uri.parse(attachmentUri), "r").createInputStream();
                size += fileInput.available();
                size += 2; // \r\n (end)
            }

            if (progressDialog != null) {
                final int fsize = size;
                NewMessageActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        progressDialog.setMax(fsize);
                    }
                });
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
                while ((length = fileInput.read(buffer, 0, 4096)) != -1 && progressDialogCancel == false) {
                    out.write(buffer, 0, length);
                    total += length;
                    if (((int) (total / 102400)) != totallast) {
                        totallast = (int) (total / 102400);
                        progressHandler.sendEmptyMessage(total);
                    }
                }
                if (progressDialogCancel == false) {
                    out.write(end.getBytes());
                }
                fileInput.close();
                progressHandler.sendEmptyMessage(size);
            }
            if (progressDialogCancel == false) {
                out.write(outStrEndB);
                out.flush();
            }
            out.close();

            if (progressDialogCancel) {
                return false;
            } else {
                return (conn.getResponseCode() == 200);
            }
        } catch (Exception e) {
            Log.e("sendOpinion", e.toString());
        }
        return false;
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
            if (requestCode == ACTIVITY_TAGS) {
                etMessage.setText("*" + data.getStringExtra("tag") + " " + etMessage.getText());
            } else if (requestCode == ACTIVITY_LOCATION) {
                pid = data.getIntExtra("pid", 0);
                lat = data.getDoubleExtra("lat", 0);
                lon = data.getDoubleExtra("lon", 0);
                acc = data.getIntExtra("acc", 0);
                pname = data.getStringExtra("pname");
                if ((pid > 0 || lat != 0) && pname != null) {
                    bLocation.setSelected(true);
                }
            } else if ((requestCode == ACTIVITY_ATTACHMENT_IMAGE || requestCode == ACTIVITY_ATTACHMENT_VIDEO) && data != null) {
                attachmentUri = data.getDataString();
                attachmentMime = (requestCode == ACTIVITY_ATTACHMENT_IMAGE) ? "image/jpeg" : "video/3gpp";
                bAttachment.setSelected(true);
            }
        }
    }

    class AttachAdapter extends BaseAdapter {

        private Context context;
        private final int icons[] = {
            R.drawable.ic_attach_photo,
            R.drawable.ic_attach_photo_new,
            R.drawable.ic_attach_video,
            R.drawable.ic_attach_video_new
        };
        private final int labels[] = {
            R.string.Photo_from_gallery,
            R.string.New_photo,
            R.string.Video_from_gallery,
            R.string.New_video
        };

        public AttachAdapter(Context context) {
            super();
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = (TextView) convertView;
            if (tv == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                tv = (TextView) vi.inflate(android.R.layout.simple_list_item_1, null);
                tv.setCompoundDrawablePadding(10);
                tv.setTextColor(Color.BLACK);
            }
            if (position >= 0 && position < 4) {
                tv.setText(labels[position]);
                tv.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0);
            }
            return tv;
        }

        public int getCount() {
            return labels.length;
        }

        public long getItemId(int position) {
            return position;
        }

        public Object getItem(int position) {
            if (position >= 0 && position < 4) {
                return context.getResources().getString(labels[position]);
            } else {
                return "";
            }
        }
    }
}
