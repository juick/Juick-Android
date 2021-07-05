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

package com.juick;

import android.accounts.AccountManager;
import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juick.android.ErrorReporter;
import com.juick.android.JuickConfig;
import com.juick.android.LinkPreviewer;
import com.juick.android.NotificationSender;
import com.juick.android.SignInProvider;
import com.juick.android.Utils;
import com.juick.api.Api;
import com.juick.api.UpLoadProgressInterceptor;
import com.juick.api.model.Chat;
import com.juick.api.model.Post;
import com.juick.api.model.PostResponse;
import com.juick.api.model.SecureUser;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by gerc on 14.02.2016.
 */
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

    public interface MessageListener {
        void onMessageSent(Post newMessage);
    }

    public interface ChatsListener {
        void onChatsReceived(List<Chat> chats);
    }

    private OnProgressListener callback;

    public void setOnProgressListener(OnProgressListener callback) {
        this.callback = callback;
    }

    private AuthorizationListener authorizationCallback;

    public void setAuthorizationCallback(AuthorizationListener authorizationCallback) {
        this.authorizationCallback = authorizationCallback;
    }

    private ErrorReporter errorReporter;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (!BuildConfig.DEBUG) {
            errorReporter = new ErrorReporter(this, "support@juick.com", getString(R.string.appCrash));
        }
        JuickConfig.init();
    }


    private JacksonConverterFactory jacksonConverterFactory;

    public JacksonConverterFactory getJacksonConverterFactory() {
        if (jacksonConverterFactory == null) {
            jacksonConverterFactory = JacksonConverterFactory.create(getJsonMapper());
        }
        return jacksonConverterFactory;
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
                    .addConverterFactory(getJacksonConverterFactory())
                    .build();

            api = retrofit.create(Api.class);
        }
        return api;
    }

    public void auth(String username, String password, Callback<SecureUser> callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator((route, response) -> {
                    if (response.request().header("Authorization") != null) {
                        return null; // Give up, we've already failed to authenticate.
                    }
                    String basicAuth = Credentials.basic(username, password);
                    return response.request().newBuilder()
                            .header("Authorization", basicAuth)
                            .build();
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_ENDPOINT)
                .client(client)
                .addConverterFactory(getJacksonConverterFactory())
                .build();
        retrofit.create(Api.class).me().enqueue(callback);
    }

    public void sendMessage(String txt, Uri attachmentUri, String attachmentMime, MessageListener messageListener) {
        MultipartBody.Part body = null;
        if (attachmentUri != null) {
            Log.d("sendMessage", attachmentMime + " " + attachmentUri.toString());
            try (InputStream stream = getContentResolver().openInputStream(attachmentUri)) {
                RequestBody requestFile =
                        RequestBody.create(MediaType.parse("multipart/form-data"), IOUtils.toByteArray(stream));
                body = MultipartBody.Part.createFormData("attach", String.format("attach.%s", MimeTypeMap.getSingleton().getExtensionFromMimeType(attachmentMime)), requestFile);
            } catch (IOException e) {
                Log.w("sendMessage", "attachment failed", e);
            }
        }
        App.getInstance().getApi().newPost(RequestBody.create(MediaType.parse("text/plain"), txt),
                body
        ).enqueue(new Callback<PostResponse>() {
            @Override
            public void onResponse(@NonNull Call<PostResponse> call, @NonNull retrofit2.Response<PostResponse> response) {
                if (response.isSuccessful()) {
                    messageListener.onMessageSent(response.body().getNewMessage());
                } else {
                    messageListener.onMessageSent(null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PostResponse> call, @NonNull Throwable t) {
                messageListener.onMessageSent(null);
            }
        });
    }

    public ObjectMapper getJsonMapper() {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return jsonMapper;
    }

    private List<LinkPreviewer> linkPreviewers;

    public List<LinkPreviewer> getPreviewers() {
        if (linkPreviewers == null) {
            linkPreviewers = new ArrayList<>();
        }
        return linkPreviewers;
    }

    public boolean hasViewableContent(String message) {
        for (LinkPreviewer previewer : getPreviewers()) {
            if (previewer.hasViewableContent(message)) {
                return true;
            }
        }
        return false;
    }

    private SignInProvider signInProvider;

    public SignInProvider getSignInProvider() {
        return signInProvider;
    }

    public void setSignInProvider(SignInProvider provider) {
        signInProvider = provider;
    }

    private NotificationSender notificationSender;

    public NotificationSender getNotificationSender() {
        if (notificationSender == null) {
            notificationSender = new NotificationSender(this);
        }
        return notificationSender;
    }
}
