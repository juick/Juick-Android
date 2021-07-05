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
package com.juick.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.juick.App;
import com.juick.R;
import com.juick.util.StringUtils;

import java.util.Arrays;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class Utils {
    public static int myId = 0;

    public static @Nullable Account getAccount() {
        AccountManager am = AccountManager.get(App.getInstance());
        Account accs[] = am.getAccountsByType(App.getInstance().getString(R.string.com_juick));
        return accs.length > 0 ? accs[0] : null;
    }

    public static boolean hasAuth() {
        return getAccount() != null;
    }

    public static @Nullable String getNick() {
        Account account = getAccount();
        return account != null ? account.name : null;
    }

    public static @Nullable Bundle getAccountData() {
        AccountManager am = AccountManager.get(App.getInstance());
        Account account = getAccount();
        if (account != null) {
            Bundle b = null;
            try {
                b = am.getAuthToken(account, StringUtils.EMPTY, null, false, null, null).getResult();
            } catch (Exception e) {
                Log.d("getBasicAuthString", Log.getStackTraceString(e));
            }
            return b;
        }
        return null;
    }

    private static OkHttpClient.Builder WSFactoryInstance;

    public static OkHttpClient.Builder getSSEFactory() {
        if (WSFactoryInstance == null) {
            WSFactoryInstance = new OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(App.getInstance().getOkHttpLegacyTls(), ConnectionSpec.CLEARTEXT));
        }
        return WSFactoryInstance;
    }

    public static boolean isImageTypeAllowed(String mime) {
        return mime != null && (mime.equals("image/jpeg") || mime.equals("image/png"));
    }

    public static String getMimeTypeFor(Context context, Uri url){
        ContentResolver resolver = context.getContentResolver();
        return resolver.getType(url);
    }

    public static void updateFCMToken(String prefToken) {
        final String TAG = "updateFCMToken";
        Log.d(TAG, "currentToken " + prefToken);
        if (hasAuth()) {
            if (prefToken != null) {
                App.getInstance().getApi().registerPush(prefToken).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Log.d(TAG, "registerPush " + response.code());
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Log.d(TAG, "Failed to register", t);
                    }
                });
            }
        }
    }
}
