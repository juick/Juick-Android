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
package com.juick.android

import android.net.UrlQuerySanitizer
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.api.ext.YouTube
import com.juick.api.model.LinkPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.regex.Pattern

class YouTubePreviewer : LinkPreviewer {
    private val youTube: YouTube

    init {
        val youTubeClient = OkHttpClient.Builder()
            .build()
        val youTubeMapper = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(youTubeClient)
            .addConverterFactory(App.instance.jacksonConverterFactory)
            .build()
        youTube = youTubeMapper.create(YouTube::class.java)
    }

    override fun hasViewableContent(message: String): Boolean {
        return youtubeLink.matcher(message).find()
    }

    override fun getPreviewUrl(message: String, callback: UrlCallback) {
        val youtubeMatcher = youtubeLink.matcher(message)
        if (youtubeMatcher.find()) {
            val linkMatcher = youtubeLink.matcher(youtubeMatcher.group())
            if (linkMatcher.matches() && linkMatcher.groupCount() > 1) {
                var videoId = linkMatcher.group(1)
                if (videoId == null && linkMatcher.groupCount() > 2) {
                    // have query string
                    val sanitizer = UrlQuerySanitizer(youtubeMatcher.group())
                    sanitizer.allowUnregisteredParamaters = true
                    videoId = sanitizer.getValue("v")
                }
                if (videoId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val playlist = youTube.getDescription(
                                videoId,
                                App.instance.getText(R.string.google_api_key).toString()
                            )
                            withContext(Dispatchers.Main) {
                                playlist.items.firstOrNull()?.let { video ->
                                    val thumbnail = video.snippet.thumbnails["default"]
                                    if (thumbnail != null) {
                                        callback.invoke(
                                            LinkPreview(
                                                linkMatcher.group(0)!!,
                                                thumbnail.url,
                                                video.snippet.title
                                            )
                                        )
                                    } else {
                                        callback.invoke(null)
                                    }
                                } ?: callback.invoke(null)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback.invoke(null)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val youtubeLink = Pattern.compile(
            "(?:https?:)?\\/\\/(?:www\\.|m\\.|gaming\\.)?(?:youtu(?:(?:\\.be\\/|be\\.com\\/(?:v|embed)\\/)([-\\w]+)|be\\.com\\/watch)((?:(?:\\?|&(?:amp;)?)(?:\\w+=[-\\.\\w]*[-\\w]))*)|youtube\\.com\\/playlist\\?list=([-\\w]*)(&(amp;)?[-\\w\\?=]*)?)",
            Pattern.MULTILINE
        )
    }
}