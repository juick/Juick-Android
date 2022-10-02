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

import com.juick.api.model.*
import com.juick.api.model.Tag
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by gerc on 14.02.2016.
 */
interface Api {
    @GET("me")
    suspend fun me(): SecureUser

    @GET
    suspend fun getPosts(@Url url: String): List<Post>

    @GET("pm")
    suspend fun pm(@Query("uname") uname: String): List<Post>

    @FormUrlEncoded
    @POST("pm")
    suspend fun postPm(@Query("uname") uname: String, @Field("body") body: String): Post

    @Multipart
    @POST("post")
    suspend fun newPost(
        @Part("body") body: RequestBody?,
        @Part file: MultipartBody.Part?
    ): PostResponse

    @GET("tags")
    suspend fun tags(): List<Tag>

    @GET("thread")
    suspend fun thread(@Query("mid") messageId: Int): List<Post>

    @GET("android/register")
    suspend fun registerPush(@Query("regid") login: String)

    @GET("groups_pms")
    suspend fun groupsPms(@Query("cnt") cnt: Int): Pms

    @FormUrlEncoded
    @POST("_google")
    suspend fun googleAuth(@Field("idToken") token: String): AuthResponse

    @FormUrlEncoded
    @POST("signup")
    suspend fun signup(
        @Field("username") username: String?, @Field("password") password: String?,
        @Field("verificationCode") verificationCode: String?
    )
}
