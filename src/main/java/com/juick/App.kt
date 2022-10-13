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
package com.juick

import android.accounts.AccountManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.juick.android.*
import com.juick.android.SignInActivity.SignInStatus
import com.juick.android.Utils.accountData
import com.juick.api.Api
import com.juick.api.RequestBodyUtil
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.api.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.FileNotFoundException

/**
 * Created by gerc on 14.02.2016.
 */
class App : MultiDexApplication() {
    val api: Api by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method(), original.body())
                val accountData = accountData
                if (accountData != null) {
                    val hash = accountData.getString(AccountManager.KEY_AUTHTOKEN)
                    if (!TextUtils.isEmpty(hash)) {
                        requestBuilder.addHeader("Authorization", "Juick $hash")
                    }
                }
                val request = requestBuilder.build()
                val response = chain.proceed(request)
                if (!response.isSuccessful) {
                    if (response.code() == 401) {
                        if (accountData != null) {
                            authorizationCallback?.invoke()
                        }
                    }
                }
                response
            }
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_ENDPOINT)
            .client(client)
            .addConverterFactory(jacksonConverterFactory)
            .build()
        retrofit.create(Api::class.java)
    }
    val jsonMapper: ObjectMapper by lazy {
        val mapper = jacksonObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper
    }

    var authorizationCallback: (() -> Unit)? = null

    private var errorReporter: ErrorReporter? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        val errorSubject =
            "[${getString(R.string.Juick)}] [${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})] ${
                getString(R.string.appCrash)
            }"
        if (!BuildConfig.DEBUG) {
            errorReporter = ErrorReporter(this, "support@juick.com", errorSubject)
        }

        init()
    }

    internal val jacksonConverterFactory: JacksonConverterFactory by lazy {
        JacksonConverterFactory.create(jsonMapper)
    }

    suspend fun auth(username: String, password: String): User {
        val client = OkHttpClient.Builder()
            .authenticator { _, response ->
                if (response.request().header("Authorization") != null) {
                    return@authenticator null // Give up, we've already failed to authenticate.
                }
                val basicAuth = Credentials.basic(username, password)
                response.request().newBuilder()
                    .header("Authorization", basicAuth)
                    .build()
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_ENDPOINT)
            .client(client)
            .addConverterFactory(jacksonConverterFactory)
            .build()
        return retrofit.create(Api::class.java).me()
    }

    suspend fun sendMessage(
        txt: String?,
        messageListener: (PostResponse) -> Unit
    ) {
        sendMessage(txt, null, null, messageListener)
    }

    @Throws(FileNotFoundException::class)
    suspend fun sendMessage(
        txt: String?,
        attachmentUri: Uri?,
        attachmentMime: String?,
        messageListener: (PostResponse) -> Unit
    ) {
        var body: MultipartBody.Part? = null
        if (attachmentUri != null) {
            Log.d("sendMessage", "$attachmentMime $attachmentUri")
            contentResolver.openInputStream(attachmentUri)?.let { stream ->
                val requestFile =
                    RequestBodyUtil.create(MediaType.parse("multipart/form-data"), stream)
                body = MultipartBody.Part.createFormData(
                    "attach",
                    String.format(
                        "attach.%s",
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(attachmentMime)
                    ),
                    requestFile
                )
            }
        }
        try {
            val message = withContext(Dispatchers.IO) {
                instance.api.newPost(
                    RequestBody.create(MediaType.parse("text/plain"), txt ?: ""),
                    body
                )
            }
            messageListener.invoke(message)
        } catch (e: Exception) {
            messageListener.invoke(PostResponse(null, e.message ?: "", ""))
        }
    }

    val previewers: ArrayList<LinkPreviewer> = arrayListOf()

    fun hasViewableContent(message: String): Boolean {
        for (previewer in previewers) {
            if (previewer.hasViewableContent(message)) {
                return true
            }
        }
        return false
    }

    var signInProvider: SignInProvider? = null
    val notificationSender: NotificationSender by lazy {
        NotificationSender(instance, jsonMapper)
    }
    val messages = MutableStateFlow<List<Post>>(listOf())
    val signInStatus = MutableStateFlow(SignInStatus.SIGNED_OUT)

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        lateinit var instance: App
            private set
    }
}