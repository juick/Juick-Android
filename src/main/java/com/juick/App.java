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

package com.juick;

import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatDelegate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juick.android.Utils;
import com.juick.api.Api;
import com.juick.api.UpLoadProgressInterceptor;
import com.juick.api.model.SecureUser;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by gerc on 14.02.2016.
 */
@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "support@juick.com", resSubject = R.string.appCrash, reportFileName = "ACRA-report.txt")
public class App extends Application {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    private Api api;

    private ObjectMapper jsonMapper;

    public interface OnProgressListener {
        void onProgress(long progressPercentage);
    }

    public interface AuthorizationListener {
        void onUnauthorized();
    }

    private OnProgressListener callback;

    public void setOnProgressListener(OnProgressListener callback) {
        this.callback = callback;
    }

    private AuthorizationListener authorizationCallback;

    public void setAuthorizationCallback(AuthorizationListener authorizationCallback) {
        this.authorizationCallback = authorizationCallback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }
    }

    public Api getApi() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new UpLoadProgressInterceptor((bytesWritten, contentLength) -> {
                        if (callback != null) {
                            callback.onProgress(100 * bytesWritten / contentLength);
                        }
                    }))
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Accept", "application/json")
                                .method(original.method(), original.body());
                        Bundle accountData = Utils.getAccountData();
                        if (accountData != null) {
                            String hash = accountData.getString(AccountManager.KEY_AUTHTOKEN);
                            if (!TextUtils.isEmpty(hash)) {
                                requestBuilder.addHeader("Authorization", "Juick " + hash);
                            }
                        }
                        Request request = requestBuilder.build();
                        Response response = chain.proceed(request);
                        if (!response.isSuccessful()) {
                            if (response.code() == 401) {
                                if (authorizationCallback != null) {
                                    if (accountData != null) {
                                        authorizationCallback.onUnauthorized();
                                    }
                                }
                            }
                        }
                        return response;
                    })
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_ENDPOINT)
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create(getJsonMapper()))
                    .build();

            api = retrofit.create(Api.class);
        }
        return api;
    }

    public void auth(String username, String password, Callback<SecureUser> callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator((route, response) -> {
                    String basicAuth = Credentials.basic(username, password);
                    return response.request().newBuilder()
                            .header("Authorization", basicAuth)
                            .build();
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_ENDPOINT)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(getJsonMapper()))
                .build();
        retrofit.create(Api.class).me().enqueue(callback);
    }

    public ObjectMapper getJsonMapper() {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return jsonMapper;
    }
}
