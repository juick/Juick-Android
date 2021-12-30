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

package com.juick.api;

import androidx.annotation.NonNull;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class UpLoadProgressInterceptor implements Interceptor {
    private final CountingRequestBody.Listener progressListener;

    public UpLoadProgressInterceptor(CountingRequestBody.Listener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    @NonNull
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        if (originalRequest.body() == null) {
            return chain.proceed(originalRequest);
        }

        Request progressRequest = originalRequest.newBuilder()
                .method(originalRequest.method(),
                        new CountingRequestBody(originalRequest.body(), progressListener))
                .build();

        return chain.proceed(progressRequest);
    }
}
