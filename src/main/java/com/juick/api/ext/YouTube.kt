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
package com.juick.api.ext

import retrofit2.http.GET
import com.juick.api.ext.youtube.VideoList
import retrofit2.Call
import retrofit2.http.Query

interface YouTube {
    @GET("videos?part=snippet")
    suspend fun getDescription(
        @Query("id") videoId: String?,
        @Query("key") apiKey: String?
    ): VideoList
}