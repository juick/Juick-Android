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

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class UpLoadProgressInterceptor(private val progressListener: (Long, Long) -> Unit) :
    Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.body() == null) {
            return chain.proceed(originalRequest)
        }
        val progressRequest = originalRequest.newBuilder()
            .method(
                originalRequest.method(),
                CountingRequestBody(originalRequest.body()!!, progressListener)
            )
            .build()
        return chain.proceed(progressRequest)
    }
}