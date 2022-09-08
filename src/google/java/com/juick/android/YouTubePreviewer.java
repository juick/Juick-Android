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

package com.juick.android;

import android.net.UrlQuerySanitizer;

import androidx.annotation.NonNull;

import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.api.ext.YouTube;
import com.juick.api.ext.youtube.Thumbnail;
import com.juick.api.ext.youtube.Video;
import com.juick.api.ext.youtube.VideoList;
import com.juick.api.model.LinkPreview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class YouTubePreviewer implements LinkPreviewer {

    private final YouTube youTube;

    public YouTubePreviewer() {
        OkHttpClient youTubeClient = new OkHttpClient.Builder()
                .build();
        Retrofit youTubeMapper = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/youtube/v3/")
                .client(youTubeClient)
                .addConverterFactory(App.getInstance().getJacksonConverterFactory())
                .build();

        youTube = youTubeMapper.create(YouTube.class);
    }

    private static final Pattern youtubeLink = Pattern.compile("(?:https?:)?\\/\\/(?:www\\.|m\\.|gaming\\.)?(?:youtu(?:(?:\\.be\\/|be\\.com\\/(?:v|embed)\\/)([-\\w]+)|be\\.com\\/watch)((?:(?:\\?|&(?:amp;)?)(?:\\w+=[-\\.\\w]*[-\\w]))*)|youtube\\.com\\/playlist\\?list=([-\\w]*)(&(amp;)?[-\\w\\?=]*)?)", Pattern.MULTILINE);
    public boolean hasViewableContent(String message) {
        return youtubeLink.matcher(message).find() && !BuildConfig.DEBUG;
    }
    public void getPreviewUrl(String message, UrlCallback callback) {
        Matcher youtubeMatcher = youtubeLink.matcher(message);
        if (youtubeMatcher.find()) {
            Matcher linkMatcher = youtubeLink.matcher(youtubeMatcher.group());
            if (linkMatcher.matches() && linkMatcher.groupCount() > 1) {
                String videoId = linkMatcher.group(1);
                if (videoId == null && linkMatcher.groupCount() > 2) {
                    // have query string
                    UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(youtubeMatcher.group());
                    sanitizer.setAllowUnregisteredParamaters(true);
                    videoId = sanitizer.getValue("v");
                }
                if (videoId != null) {
                    youTube.getDescription(videoId,
                            App.getInstance().getText(R.string.google_api_key).toString()).enqueue(new Callback<VideoList>() {
                        @Override
                        public void onResponse(@NonNull Call<VideoList> call, @NonNull Response<VideoList> response) {
                            if (response.isSuccessful()) {
                                VideoList playlist = response.body();
                                if (playlist != null && playlist.getItems().size() > 0) {
                                    Video video = playlist.getItems().get(0);
                                    if (video != null) {
                                        Thumbnail thumbnail = video.getSnippet().getThumbnails().get("default");
                                        if (thumbnail != null) {
                                            callback.response(new LinkPreview(thumbnail.getUrl(), video.getSnippet().getTitle()));
                                        } else {
                                            callback.response(null);
                                        }
                                    } else {
                                        callback.response(null);
                                    }
                                } else {
                                    callback.response(null);
                                }
                            } else {
                                callback.response(null);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<VideoList> call, @NonNull Throwable t) {
                            callback.response(null);
                        }
                    });

                }
            }
        }
    }
}
