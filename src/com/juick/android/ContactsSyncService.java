/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
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
import com.juick.R;
import com.juick.android.api.JuickUser;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;

/**
 *
 * @author ugnich
 */
public class ContactsSyncService extends Service {

    private static SyncAdapterImpl sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

        private Context mContext;

        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            try {
                ContactsSyncService.performSync(mContext, account, extras, authority, provider, syncResult);
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

        final String jsonStr = Utils.getJSON(context, "http://api.juick.com/users/friends");
        if (jsonStr != null && jsonStr.length() > 4) {
            try {
                JSONArray json = new JSONArray(jsonStr);
                int cnt = json.length();
                for (int i = 0; i < cnt; i++) {
                    JuickUser user = JuickUser.parseJSON(json.getJSONObject(i));
                    if (!localContacts.containsKey(user.UName)) {
                        addContact(context, account, user);
                    }
                }
            } catch (JSONException e) {
            }
        }
    }

    private static void addContact(Context context, Account account, JuickUser user) {
//     Log.i(TAG, "Adding contact: " + name);
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

        //Create our RawContact
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
        builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
        builder.withValue(RawContacts.SYNC1, user.UName);
        operationList.add(builder.build());

        // Nickname
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.CommonDataKinds.Nickname.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME, user.UName);
        operationList.add(builder.build());

        // StructuredName
        if (user.FullName != null) {
            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
            builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.FullName);
            operationList.add(builder.build());
        }

        // Photo
        Bitmap photo = Utils.downloadImage("http://i.juick.com/a/" + user.UID + ".png");
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
        builder.withValue(ContactsContract.Data.DATA1, user.UID);
        builder.withValue(ContactsContract.Data.DATA2, user.UName);
        builder.withValue(ContactsContract.Data.DATA3, context.getString(R.string.Juick_profile));
        builder.withValue(ContactsContract.Data.DATA4, user.UName);
        operationList.add(builder.build());

        try {
            mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
//      Log.e(TAG, "Something went wrong during creation! " + e);
            e.printStackTrace();
        }
    }
}


