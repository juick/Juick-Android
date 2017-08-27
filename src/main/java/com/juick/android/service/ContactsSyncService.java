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
package com.juick.android.service;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.juick.R;
import com.juick.api.RestClient;
import com.juick.api.model.User;
import retrofit2.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Ugnich Anton
 */
public class ContactsSyncService extends Service {

    private static SyncAdapterImpl sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

        public SyncAdapterImpl(Context context) {
            super(context, true);
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            try {
                ContactsSyncService.performSync(getContext(), account, extras, authority, provider, syncResult);
            } catch (OperationCanceledException e) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return getSyncAdapter().getSyncAdapterBinder();
    }

    private SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null) {
            sSyncAdapter = new SyncAdapterImpl(this);
        }
        return sSyncAdapter;
    }

    private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) throws OperationCanceledException {
        HashMap<String, Long> localContacts = new HashMap<String, Long>();
        mContentResolver = context.getContentResolver();

        // Load the local contacts
        Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build();
        Cursor c1 = mContentResolver.query(rawContactUri, new String[]{BaseColumns._ID, RawContacts.SYNC1}, null, null, null);
        while (c1.moveToNext()) {
            localContacts.put(c1.getString(1), c1.getLong(0));
        }

        try {
            Response<List<User>> response = RestClient.getApi().getFriends().execute();
            if (response.isSuccessful()) {
                List<User> friends = response.body();
                if (friends != null) {
                    for (User user : friends) {
                        if (!localContacts.containsKey(user.uname)) {
                            addContact(context, account, user);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addContact(Context context, Account account, User user) {
//     Log.i(TAG, "Adding contact: " + name);
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

        //Create our RawContact
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
        builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
        builder.withValue(RawContacts.SYNC1, user.uname);
        operationList.add(builder.build());

        // Nickname
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.CommonDataKinds.Nickname.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME, user.uname);
        operationList.add(builder.build());

        // StructuredName
        if (user.fullname != null) {
            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
            builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.fullname);
            operationList.add(builder.build());
        }

        Bitmap photo = null;
        try {
            photo = Glide.with(context).load("http://i.juick.com/a/" + user.uid + ".png").asBitmap()
                    .centerCrop()
                    .into(200, 200)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w("JuickContacts", "Avatar error", e);
        }
        // Photo
        if (photo != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, baos);
            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference(ContactsContract.CommonDataKinds.Photo.RAW_CONTACT_ID, 0);
            builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
            builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, baos.toByteArray());
            operationList.add(builder.build());
        }

        // link to profile
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.com.juick.profile");
        builder.withValue(ContactsContract.Data.DATA1, user.uid);
        builder.withValue(ContactsContract.Data.DATA2, user.uname);
        builder.withValue(ContactsContract.Data.DATA3, context.getString(R.string.Juick_profile));
        builder.withValue(ContactsContract.Data.DATA4, user.uname);
        operationList.add(builder.build());

        try {
            mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
//      Log.e(TAG, "Something went wrong during creation! " + e);
            e.printStackTrace();
        }
    }
}
