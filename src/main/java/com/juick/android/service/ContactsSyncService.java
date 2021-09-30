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
package com.juick.android.service;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.juick.App;
import com.juick.R;
import com.juick.api.GlideApp;
import com.juick.api.model.SecureUser;
import com.juick.api.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import retrofit2.Response;

/**
 * @author Ugnich Anton
 */
public class ContactsSyncService extends Service {

    private JuickSyncAdapter contactsSyncAdapter;

    private static class JuickSyncAdapter extends AbstractThreadedSyncAdapter {

        private final Context context;

        JuickSyncAdapter(Context context) {
            super(context, true);
            this.context = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                                  ContentProviderClient provider, SyncResult syncResult) {
            try {
                Response<SecureUser> response = App.getInstance().getApi().me().execute();
                if (response.isSuccessful()) {
                    SecureUser me = response.body();
                    List<User> friends = me.getRead();
                    if (friends != null) {
                        updateContacts(account, friends);
                    }
                }
            } catch (IOException e) {
                Log.d(ContactsSyncService.class.getSimpleName(), "Sync error", e);
            }
        }

        private void updateContacts(Account account, List<User> users) {
            Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
                    .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                    .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build();
            Cursor queryResult = context.getContentResolver().query(rawContactUri, new String[]{
                    BaseColumns._ID, RawContacts.SYNC1}, null, null, null);
            if (queryResult != null) {
                while (queryResult.moveToNext()) {
                    Uri contactsUri = RawContacts.CONTENT_URI.buildUpon()
                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                            .build();
                    context.getContentResolver().delete(contactsUri, ContactsContract.RawContacts._ID + " = ?",
                            new String[] { String.valueOf(queryResult.getLong(0)) });
                }
                queryResult.close();
            }
            for (User user : users) {
                // NOTE: single applyBatch will fail with TransactionTooLargeException
                ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
                //Create our RawContact
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
                builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
                builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
                builder.withValue(RawContacts.SYNC1, user.getUname());
                operationList.add(builder.build());

                // Nickname
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builder.withValueBackReference(ContactsContract.CommonDataKinds.Nickname.RAW_CONTACT_ID, 0);
                builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
                builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME, user.getUname());
                operationList.add(builder.build());

                // StructuredName
                if (user.getFullname() != null) {
                    builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                    builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
                    builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                    builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.getFullname());
                    operationList.add(builder.build());
                }

                Bitmap photo = null;
                if (user.getAvatar() != null) {
                    try {
                        photo = GlideApp.with(getContext()).asBitmap().load(user.getAvatar())
                                .placeholder(R.drawable.av_96)
                                .submit()
                                .get();
                    } catch (InterruptedException | ExecutionException e) {
                        Log.w(ContactsSyncService.class.getSimpleName(), "Avatar error", e);
                    }
                }
                // Photo
                if (photo != null) {
                    ByteArrayOutputStream photoData = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.PNG, 100, photoData);
                    builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                    builder.withValueBackReference(ContactsContract.CommonDataKinds.Photo.RAW_CONTACT_ID, 0);
                    builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                    builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoData.toByteArray());
                    operationList.add(builder.build());
                }

                // link to profile
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
                builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.com.juick.profile");
                builder.withValue(ContactsContract.Data.DATA1, user.getUid());
                builder.withValue(ContactsContract.Data.DATA2, getContext().getString(R.string.com_juick));
                builder.withValue(ContactsContract.Data.DATA3, user.getUname());
                builder.withValue(ContactsContract.Data.DATA4, getContext().getString(R.string.Juick_profile));
                operationList.add(builder.build());
                try {
                    context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
                } catch (Exception e) {
                    Log.d(ContactsSyncService.class.getSimpleName(), "Sync error", e);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        contactsSyncAdapter = new JuickSyncAdapter(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return contactsSyncAdapter.getSyncAdapterBinder();
    }
}
