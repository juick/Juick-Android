/*
 * Copyright (C) 2008-2024, Juick
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

import accountData
import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.juick.android.*
import com.juick.api.Api
import com.juick.api.RequestBodyUtil
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.api.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

/**
 * Created by gerc on 14.02.2016.
 */
class App : Application() {
    val api: Api by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "${getString(R.string.Juick)}/${BuildConfig.VERSION_CODE} " +
                            "okhttp/${OkHttp.VERSION} Android/${Build.VERSION.SDK_INT}")
                    .method(original.method, original.body)
                if (accountData.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Juick $accountData")
                }
                val request = requestBuilder.build()
                val response = chain.proceed(request)
                if (!response.isSuccessful) {
                    if (response.code == 401) {
                        if (accountData.isNotEmpty()) {
                            authorizationCallback?.invoke()
                        }
                    }
                }
                response
            }
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_ENDPOINT)
            .client(client)
            .addConverterFactory(kotlinxSerializationConverterFactory)
            .build()
        retrofit.create(Api::class.java)
    }
    val jsonMapper = Json  {
        ignoreUnknownKeys = true
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
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleManager)
        init()
    }

    internal val kotlinxSerializationConverterFactory: Converter.Factory by lazy {
        jsonMapper.asConverterFactory(
            "application/json; charset=UTF8".toMediaType())
    }

    suspend fun auth(username: String, password: String): User {
        val client = OkHttpClient.Builder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    return@authenticator null // Give up, we've already failed to authenticate.
                }
                val basicAuth = Credentials.basic(username, password)
                response.request.newBuilder()
                    .header("Authorization", basicAuth)
                    .build()
            }
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_ENDPOINT)
            .client(client)
            .addConverterFactory(kotlinxSerializationConverterFactory)
            .build()
        return retrofit.create(Api::class.java).me()
    }

    @Throws(FileNotFoundException::class)
    fun sendMessage(
        scope: CoroutineScope,
        receiver: MutableStateFlow<Result<PostResponse>?>,
        txt: String? = null,
        attachmentUri: Uri? = null,
        attachmentMime: String? = null
    ) {
        var body: MultipartBody.Part? = null
        if (attachmentUri != null) {
            Log.d("sendMessage", "$attachmentMime $attachmentUri")
            contentResolver.openInputStream(attachmentUri)?.let { stream ->
                val requestFile =
                    RequestBodyUtil.create("multipart/form-data".toMediaTypeOrNull(), stream)
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
        scope.launch {
            receiver.update {
                runCatching {
                    instance.api.newPost(
                        (txt ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                        body
                    )
                }
            }
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
        NotificationSender(instance)
    }
    val messages = MutableStateFlow<List<Post>>(listOf())

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        lateinit var instance: App
            private set
    }

    object AppLifecycleManager : DefaultLifecycleObserver {
        var isInForeground = true

        override fun onStart(owner: LifecycleOwner) {
            isInForeground = true
        }

        override fun onStop(owner: LifecycleOwner) {
            isInForeground = false
        }
    }
}