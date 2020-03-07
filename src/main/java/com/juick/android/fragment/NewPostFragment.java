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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.juick.App;
import com.juick.R;
import com.juick.android.NewMessageActivity;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.databinding.FragmentNewPostsBinding;
import com.juick.util.StringUtils;

/**
 * Created by alx on 02.01.17.
 */

public class NewPostFragment extends BaseFragment {
    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;
    private String attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog;
    private NewMessageActivity.BooleanReference progressDialogCancel = new NewMessageActivity.BooleanReference(false);

    private FragmentNewPostsBinding model;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        model = FragmentNewPostsBinding.inflate(inflater, container, false);
        return model.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.New_message);

        model.buttonTags.setOnClickListener(v -> {
            TagsFragment tagsFragment = TagsFragment.newInstance(Utils.myId);
            tagsFragment.setOnTagAppliedListener(this::applyTag);
            getBaseActivity().replaceFragment(tagsFragment);
        });
        model.buttonAttachment.setOnClickListener(v -> {
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
                model.buttonAttachment.setSelected(false);
            }
        });
        model.buttonSend.setOnClickListener(v -> {
            sendMessage();
        });

        resetForm();
        handleIntent(getBaseActivity().getIntent());
    }

    public void resetForm() {
        model.editMessage.setText(StringUtils.EMPTY);
        model.buttonAttachment.setSelected(false);
        attachmentUri = null;
        attachmentMime = null;
        progressDialogCancel.bool = false;
        model.editMessage.requestFocus();
        //setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
    }

    private void setFormEnabled(boolean state) {
        model.editMessage.setEnabled(state);
        model.buttonTags.setEnabled(state);
        model.buttonAttachment.setEnabled(state);
        model.buttonSend.setEnabled(state);
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
                model.editMessage.append(extras.getString(Intent.EXTRA_TEXT));
            } else{
                attachImage(extras.get(Intent.EXTRA_STREAM).toString());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == ViewUtil.REQUEST_CODE_READ_EXTERNAL_STORAGE) {
                model.buttonAttachment.performClick();
            }
        }
    }

    private void sendMessage() {
        final String msg = model.editMessage.getText().toString();
        if (msg.length() < 3 && attachmentUri == null) {
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
            App.getInstance().setOnProgressListener(progressPercentage -> {
                if (progressDialog != null) {
                    if (progressDialog.getMax() < progressPercentage) {
                        progressDialog.setMax((int)progressPercentage);
                    } else {
                        progressDialog.setProgress((int)progressPercentage);
                    }
                }
            });
        }
        App.getInstance().sendMessage(msg, attachmentUri, attachmentMime, (success) -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            setFormEnabled(true);
            if (success) {
                resetForm();
            }
            if ((success && attachmentUri == null) || getActivity().isFinishing()) {
                Toast.makeText(getActivity(), success ? R.string.Message_posted : R.string.Error, Toast.LENGTH_LONG).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setNeutralButton(android.R.string.ok, null);
                if (success) {
                    builder.setIcon(android.R.drawable.ic_dialog_info);
                    builder.setMessage(R.string.Message_posted);
                } else {
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(R.string.Error);
                }
                builder.show();
            }
        });
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
            model.buttonAttachment.setSelected(true);
        }else{
            Toast.makeText(getActivity(), R.string.WrongImageFormat, Toast.LENGTH_LONG).show();
        }
    }

    private void applyTag(String tag) {
        model.editMessage.setText("*" + tag + " " + model.editMessage.getText());
        int textLength = model.editMessage.getText().length();
        model.editMessage.setSelection(textLength, textLength);
    }

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}
