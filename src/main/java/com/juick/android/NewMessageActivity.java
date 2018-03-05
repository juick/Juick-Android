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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.juick.R;
import com.juick.android.fragment.BaseFragment;
import com.juick.android.fragment.NewPostFragment;
import com.juick.api.RestClient;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessageActivity extends BaseActivity implements ITagable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        addFragment(NewPostFragment.newInstance(), false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NewPostFragment currentFragment = getCommonFragment();
        if(currentFragment != null){
            currentFragment.resetForm();
            currentFragment.handleIntent(intent);
        }
    }

    public static boolean sendMessage(Context context, String txt, String attachmentUri, String attachmentMime, final ProgressDialog progressDialog, final Handler progressHandler, BooleanReference progressDialogCancel) {

        try {
            MultipartBody.Part body = null;
            if(attachmentUri != null) {
                Log.d("sendMessage", attachmentMime + " " + attachmentUri);
                File file = new File(attachmentUri);
                RequestBody requestFile =
                        RequestBody.create(MediaType.parse("multipart/form-data"), file);
                body = MultipartBody.Part.createFormData("attach", file.getName(), requestFile);
            }
            return RestClient.getApi().newPost(RequestBody.create(MediaType.parse("text/plain"), txt),
                    RequestBody.create(MediaType.parse("text/plain"), "0.0"),
                    RequestBody.create(MediaType.parse("text/plain"), "0.0"),
                    body
                   ).execute().isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == NewPostFragment.ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                NewPostFragment currentFragment = getCommonFragment();
                if(currentFragment != null){
                    currentFragment.onImageAttached(data);
                }
            }
        }
    }

    private NewPostFragment getCommonFragment(){
        BaseFragment currentFragment = this.getCurrentFragment();
        if (currentFragment.getClass().getName().equals(NewPostFragment.class.getName())) {
            return (NewPostFragment)currentFragment;
        }
        return null;
    }

    @Override
    public void onTagApplied(String tag) {
        Log.d("NewMessageActivity", "onTagApplied: " + tag);
        this.getSupportFragmentManager().popBackStackImmediate();
        NewPostFragment commonFragment = getCommonFragment();
        if(commonFragment != null){
            commonFragment.applyTag(tag);
        }else{
            Log.d("NewMessageActivity", "fucking shit: ");
        }
    }

    public static class BooleanReference {

        public boolean bool;

        public BooleanReference(boolean bool) {
            this.bool = bool;
        }
    }


    @Override
    public int fragmentContainerLayoutId() {
        return R.id.fragment_container;
    }

    @Override
    public int getTabsBarLayoutId() {
        return 0;
    }
}
