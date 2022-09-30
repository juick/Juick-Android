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
package com.juick.api

import com.juick.api.model.SecureUser
import com.juick.api.model.Post
import java.lang.Void
import okhttp3.RequestBody
import okhttp3.MultipartBody
import com.juick.api.model.PostResponse
import com.juick.api.model.Pms
import com.juick.api.model.AuthResponse
import com.juick.api.model.Tag
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Created by gerc on 14.02.2016.
 */
interface Api {
    @GET("me")
    fun me(): Call<SecureUser?>?

    @GET
    fun getPosts(@Url url: String?): Call<List<Post?>?>?

    @GET("pm")
    fun pm(@Query("uname") uname: String?): Call<List<Post?>?>?

    @FormUrlEncoded
    @POST("pm")
    fun postPm(@Query("uname") uname: String?, @Field("body") body: String?): Call<Post?>?

    @FormUrlEncoded
    @POST("post")
    fun post(@Field("body") body: String?): Call<Void?>?

    @Multipart
    @POST("post")
    fun newPost(
        @Part("body") body: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Call<PostResponse?>?

    @GET("tags")
    fun tags(@Query("user_id") userId: Int): Call<List<Tag?>?>?

    @GET("thread")
    fun thread(@Query("mid") messageId: Int): Call<List<Post?>?>?

    @GET("android/register")
    fun registerPush(@Query("regid") login: String?): Call<Void?>?

    @GET("groups_pms")
    suspend fun groupsPms(@Query("cnt") cnt: Int): Pms

    @FormUrlEncoded
    @POST("_google")
    fun googleAuth(@Field("idToken") token: String?): Call<AuthResponse?>?

    @FormUrlEncoded
    @POST("signup")
    fun signup(
        @Field("username") username: String?, @Field("password") password: String?,
        @Field("verificationCode") verificationCode: String?
    ): Call<Void?>?
}