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

import com.juick.api.model.AuthResponse;
import com.juick.api.model.Pms;
import com.juick.api.model.Post;
import com.juick.api.model.SecureUser;
import com.juick.api.model.Tag;
import com.juick.api.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
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
public interface Api {

    @GET("/me")
    Call<SecureUser> me();

    @GET()
    Call<List<Post>> getPosts(@Url String url);

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
    Call<AuthResponse> googleAuth(@Field("idToken") String token);

    @FormUrlEncoded
    @POST("/signup")
    Call<Void> signup(@Field("username") String username, @Field("password") String password,
                      @Field("verificationCode") String verificationCode);
}
