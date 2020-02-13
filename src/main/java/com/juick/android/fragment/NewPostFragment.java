/*
 * Copyright (C) 2008-2020, Juick
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

package com.juick.android.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.juick.App;
import com.juick.R;
import com.juick.android.NewMessageActivity;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.RestClient;

/**
 * Created by alx on 02.01.17.
 */

public class NewPostFragment extends BaseFragment implements View.OnClickListener {
    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;
    private EditText etMessage;
    private ImageView bTags;
    private ImageView bAttachment;
    private ImageView bSend;
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RestClient.ACTION_UPLOAD_PROGRESS)) {
                if (progressDialog != null) {
                    progressHandler.sendEmptyMessage(intent.getIntExtra(RestClient.EXTRA_PROGRESS, 0));
                }
            }
        }
    };


    public static NewPostFragment newInstance() {
        NewPostFragment fragment = new NewPostFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_posts, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.New_message);
        etMessage = view.findViewById(R.id.editMessage);
        bTags = view.findViewById(R.id.buttonTags);
        bAttachment = view.findViewById(R.id.buttonAttachment);
        bSend = view.findViewById(R.id.buttonSend);

        bTags.setOnClickListener(this);
        bAttachment.setOnClickListener(this);
        bSend.setOnClickListener(this);

        resetForm();
        handleIntent(getBaseActivity().getIntent());
    }

    public void resetForm() {
        etMessage.setText("");
        bAttachment.setSelected(false);
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
        bSend.setEnabled(state);
        //setSupportProgressBarIndeterminateVisibility(state ? Boolean.FALSE : Boolean.TRUE);
    }

    private boolean isImageTypeAllowed(String mime){
        return mime != null && (mime.equals("image/jpeg") || mime.equals("image/png"));
    }

    public void handleIntent(Intent i) {
        String action = i.getAction();
        if (action != null && action.equals(Intent.ACTION_SEND)) {
            String mime = i.getType();
            Bundle extras = i.getExtras();
            if (mime.equals("text/plain")) {
                etMessage.append(extras.getString(Intent.EXTRA_TEXT));
            } else{
                attachImage(extras.get(Intent.EXTRA_STREAM).toString());
            }
        }
    }
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSend:
                sendMessage();
                break;
            case R.id.buttonTags:
                TagsFragment tagsFragment = TagsFragment.newInstance(Utils.myId);
                tagsFragment.setOnTagAppliedListener(this::applyTag);
                getBaseActivity().replaceFragment(tagsFragment);
                break;
            case R.id.buttonAttachment:
                if (Build.VERSION.SDK_INT >= 23 && getBaseActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ViewUtil.REQUEST_CODE_READ_EXTERNAL_STORAGE);
                    return;
                }
                if (attachmentUri == null) {
                    try {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        Intent chooserIntent = Intent.createChooser(photoPickerIntent, null);

                        getActivity().startActivityForResult(chooserIntent, ACTIVITY_ATTACHMENT_IMAGE);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == ViewUtil.REQUEST_CODE_READ_EXTERNAL_STORAGE) {
                bAttachment.performClick();
            }
        }
    }

    private void sendMessage() {
        final String msg = etMessage.getText().toString();
        if (msg.length() < 3) {
            Toast.makeText(getBaseActivity(), R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }
        setFormEnabled(false);
        if (attachmentUri != null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialogCancel.bool = false;
            progressDialog.setOnCancelListener(arg0 -> NewPostFragment.this.progressDialogCancel.bool = true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(0);
            progressDialog.show();
        }
        Thread thr = new Thread(() -> {
            final boolean res = NewMessageActivity.sendMessage(getActivity(), msg, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel);
            NewPostFragment.this.getBaseActivity().runOnUiThread(() -> {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                setFormEnabled(true);
                if (res) {
                    resetForm();
                }
                if ((res && attachmentUri == null) || getActivity().isFinishing()) {
                    Toast.makeText(getActivity(), res ? R.string.Message_posted : R.string.Error, Toast.LENGTH_LONG).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            });
        });
        thr.start();
    }

    public void onImageAttached(Intent data){
        if (data != null) {
            attachImage(data.getDataString());
        }
    }

    private void attachImage(String uri){
        uri = Utils.getPath(Uri.parse(uri));
        String mime = Utils.getMimeTypeFor(uri);
        if (isImageTypeAllowed(mime)) {
            attachmentUri = uri;
            attachmentMime = mime;
            bAttachment.setSelected(true);
        }else{
            Toast.makeText(getActivity(), R.string.WrongImageFormat, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(broadcastReceiver, new IntentFilter(RestClient.ACTION_UPLOAD_PROGRESS));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(broadcastReceiver);
    }

    public void applyTag(String tag) {
        etMessage.setText("*" + tag + " " + etMessage.getText());
        int textLength = etMessage.getText().length();
        etMessage.setSelection(textLength, textLength);
    }
}
