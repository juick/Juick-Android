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

package com.juick.api;

import android.util.Base64;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juick.BuildConfig;
import com.juick.android.Utils;
import com.juick.api.model.AuthToken;
import com.juick.api.model.Pms;
import com.juick.api.model.Post;
import com.juick.api.model.Tag;
import com.juick.api.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Created by gerc on 14.02.2016.
 */
public class RestClient {

    public interface OnProgressListener {
        void onProgress(long progressPercentage);
    }

    private static ObjectMapper jsonMapper;

    public static ObjectMapper getJsonMapper() {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return jsonMapper;
    }

    private OnProgressListener callback;

    public void setOnProgressListener(OnProgressListener callback) {
        this.callback = callback;
    }

    private static RestClient instance;

    private Api api;

    public Api getApi() {
        return api;
    }

    public static RestClient getInstance() {
        if (instance == null) {
            instance = new RestClient();
        }
        return instance;
    }

    private RestClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new UpLoadProgressInterceptor((bytesWritten, contentLength) -> {
                    if (callback != null) {
                        callback.onProgress(100 * bytesWritten / contentLength);
                    }
                }))
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", Utils.getBasicAuthString())
                            .header("Accept", "application/json")
                            .method(original.method(), original.body());
                    //Log.e("intercept", requestBuilder.toString());
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
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

    public void auth(String username, String password, Callback<Void> callback) {
        String credentials = username + ":" + password;
        final String basic =
                "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", basic)
                            .header("Accept", "application/json")
                            .method(original.method(), original.body());

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_ENDPOINT)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(getJsonMapper()))
                .build();
        retrofit.create(Api.class).post(null).enqueue(callback);
    }

    public interface Api {

        @GET("/me")
        Call<User> me();

        @GET()
        Call<List<Post>> getPosts(@Url String url);

        @GET("/users/friends")
        Call<List<User>> getFriends();

        @GET("/users")
        Call<List<User>> getUsers(@Query("uname") String uname);

        @GET("/pm")
        Call<List<Post>> pm(@Query("uname") String uname);

        @FormUrlEncoded
        @POST("/pm")
        Call<Post> postPm(@Query("uname") String uname, @Field("body") String body);

        @FormUrlEncoded
        @POST("/post")
        Call<Void> post(@Field("body") String body);

        @Multipart
        @POST("/post")
        Call<Void> newPost(@Part("body") RequestBody body, @Part MultipartBody.Part file);

        @GET("/tags")
        Call<List<Tag>> tags(@Query("user_id") int userId);

        @GET("/thread")
        Call<List<Post>> thread(@Query("mid") int messageId);

        @GET("/android/register")
        Call<Void> registerPush(@Query("regid") String login);

        @GET("/groups_pms")
        Call<Pms> groupsPms(@Query("cnt") int cnt);

        @FormUrlEncoded
        @POST("/_google")
        Call<AuthToken> googleAuth(@Field("idToken") String token);
        @FormUrlEncoded
        @POST("/signup")
        Call<Void> signup(@Field("username") String username, @Field("password") String password,
                          @Field("verificationCode") String verificationCode);
    }
}
