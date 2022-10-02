/*
 * Copyright (C) 2008-2022, Juick
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
package com.juick.android.service

import android.accounts.Account
import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.ContactsContract.RawContacts
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.juick.App
import com.juick.R
import com.juick.api.model.User
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutionException

/**
 * @author Ugnich Anton
 */
class ContactsSyncService : LifecycleService() {
    private lateinit var contactsSyncAdapter: JuickSyncAdapter

    private class JuickSyncAdapter constructor(private val lifecycleScope: LifecycleCoroutineScope,
                                               appContext: Context) :
        AbstractThreadedSyncAdapter(appContext, true) {
        override fun onPerformSync(
            account: Account, extras: Bundle, authority: String,
            provider: ContentProviderClient, syncResult: SyncResult
        ) {
            lifecycleScope.launch {
                try {
                    val me = App.instance.api.me()
                    me.read?.let { updateContacts(account, it) }
                } catch (e: Exception) {
                    Log.d(ContactsSyncService::class.java.simpleName, "Sync error", e)
                }
            }
        }

        private fun updateContacts(account: Account, users: List<User>) {
            val rawContactUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build()
            val queryResult = context.contentResolver.query(
                rawContactUri, arrayOf(
                    BaseColumns._ID, RawContacts.SYNC1
                ), null, null, null
            )
            if (queryResult != null) {
                while (queryResult.moveToNext()) {
                    val contactsUri = RawContacts.CONTENT_URI.buildUpon()
                        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                        .build()
                    context.contentResolver.delete(
                        contactsUri,
                        RawContacts._ID + " = ?",
                        arrayOf(queryResult.getLong(0).toString())
                    )
                }
                queryResult.close()
            }
            for (user in users) {
                // NOTE: single applyBatch will fail with TransactionTooLargeException
                val operationList = ArrayList<ContentProviderOperation>()
                //Create our RawContact
                var builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                builder.withValue(RawContacts.ACCOUNT_NAME, account.name)
                builder.withValue(RawContacts.ACCOUNT_TYPE, account.type)
                builder.withValue(RawContacts.SYNC1, user.uname)
                operationList.add(builder.build())

                // Nickname
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                builder.withValueBackReference(
                    ContactsContract.CommonDataKinds.Nickname.RAW_CONTACT_ID,
                    0
                )
                builder.withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
                )
                builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME, user.uname)
                operationList.add(builder.build())

                // StructuredName
                if (user.fullname != null) {
                    builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    builder.withValueBackReference(
                        ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID,
                        0
                    )
                    builder.withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    builder.withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        user.fullname
                    )
                    operationList.add(builder.build())
                }
                var photo: Bitmap? = null
                if (user.avatar.isNotEmpty()) {
                    try {
                        photo = Glide.with(context).asBitmap().load(user.avatar)
                            .placeholder(R.drawable.av_96)
                            .submit()
                            .get()
                    } catch (e: InterruptedException) {
                        Log.w(ContactsSyncService::class.java.simpleName, "Avatar error", e)
                    } catch (e: ExecutionException) {
                        Log.w(ContactsSyncService::class.java.simpleName, "Avatar error", e)
                    }
                }
                // Photo
                if (photo != null) {
                    val photoData = ByteArrayOutputStream()
                    photo.compress(Bitmap.CompressFormat.PNG, 100, photoData)
                    builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    builder.withValueBackReference(
                        ContactsContract.CommonDataKinds.Photo.RAW_CONTACT_ID,
                        0
                    )
                    builder.withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                    )
                    builder.withValue(
                        ContactsContract.CommonDataKinds.Photo.PHOTO,
                        photoData.toByteArray()
                    )
                    operationList.add(builder.build())
                }

                // link to profile
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                builder.withValue(
                    ContactsContract.Data.MIMETYPE,
                    "vnd.android.cursor.item/vnd.com.juick.profile"
                )
                builder.withValue(ContactsContract.Data.DATA1, user.uid)
                builder.withValue(
                    ContactsContract.Data.DATA2,
                    getContext().getString(R.string.com_juick)
                )
                builder.withValue(ContactsContract.Data.DATA3, user.uname)
                builder.withValue(
                    ContactsContract.Data.DATA4,
                    getContext().getString(R.string.Juick_profile)
                )
                operationList.add(builder.build())
                try {
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operationList)
                } catch (e: Exception) {
                    Log.d(ContactsSyncService::class.java.simpleName, "Sync error", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        contactsSyncAdapter = JuickSyncAdapter(lifecycleScope, applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return contactsSyncAdapter.syncAdapterBinder
    }
}