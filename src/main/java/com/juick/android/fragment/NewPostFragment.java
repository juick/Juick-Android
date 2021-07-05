/*
 * Copyright (C) 2008-2021, Juick
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.juick.App;
import com.juick.R;
import com.juick.android.NewMessageActivity;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.GlideApp;
import com.juick.databinding.FragmentNewPostBinding;
import com.juick.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alx on 02.01.17.
 */

public class NewPostFragment extends BaseFragment {
    private Uri attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog;
    private final NewMessageActivity.BooleanReference progressDialogCancel = new NewMessageActivity.BooleanReference(false);

    private FragmentNewPostBinding model;

    private final ActivityResultLauncher<String> attachmentLauncher;

    public NewPostFragment() {
        super(R.layout.fragment_new_post);
        attachmentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::attachImage);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = FragmentNewPostBinding.bind(view);

        getActivity().setTitle(R.string.New_message);

        model.buttonTags.setOnClickListener(v -> {
            TagsFragment tagsFragment = TagsFragment.newInstance(Utils.myId);
            tagsFragment.setOnTagAppliedListener(this::applyTag);
            getBaseActivity().addFragment(tagsFragment, true);
        });
        model.buttonAttachment.setOnClickListener(v -> {
            if (attachmentUri == null) {
                try {
                    attachmentLauncher.launch("image/*");
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                attachmentUri = null;
                attachmentMime = null;
                model.buttonAttachment.setSelected(false);
                GlideApp.with(getContext())
                        .clear(model.imagePreview);
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

    public void handleIntent(Intent i) {
        String action = i.getAction();
        if (action != null && action.equals(Intent.ACTION_SEND)) {
            String mime = i.getType();
            Bundle extras = i.getExtras();
            if (mime.equals("text/plain")) {
                model.editMessage.append(extras.getString(Intent.EXTRA_TEXT));
            } else{
                attachImage(Uri.parse(extras.get(Intent.EXTRA_STREAM).toString()));
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
        App.getInstance().sendMessage(msg, attachmentUri, attachmentMime, (newMessage) -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (newMessage == null) {
                Toast.makeText(getActivity(), R.string.Error, Toast.LENGTH_LONG).show();
            } else {
                int mid = newMessage.getMid();
                getBaseActivity().replaceFragment(ThreadFragment.newInstance(mid));
            }
        });
    }

    private void attachImage(Uri uri) {
        if (uri != null) {
            String mime = Utils.getMimeTypeFor(getContext(), uri);
            if (Utils.isImageTypeAllowed(mime)) {
                attachmentUri = uri;
                attachmentMime = mime;
                model.buttonAttachment.setSelected(true);
                try (InputStream bitmapStream = getContext().getContentResolver().openInputStream(uri)) {
                    Bitmap image = BitmapFactory.decodeStream(bitmapStream);
                    GlideApp.with(getContext())
                            .load(image)
                            .into(model.imagePreview);
                } catch (IOException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.wrong_image_format, Toast.LENGTH_LONG).show();
            }
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
