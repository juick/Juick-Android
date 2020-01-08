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

package com.juick.android.screens;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.juick.App;
import com.juick.R;
import com.juick.api.model.Post;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedViewModel extends ViewModel {

    private String apiUrl;

    public void setUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    private MutableLiveData<List<Post>> feed;

    public LiveData<List<Post>> getFeed() {
        if (feed == null) {
            feed = new MutableLiveData<>();
            loadFeed();
        }
        return feed;
    }

    private void loadFeed() {
        App.getInstance().getApi().getPosts(apiUrl)
                .enqueue(new Callback<List<Post>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Post>> call,
                                           @NonNull Response<List<Post>> response) {
                        if (response.isSuccessful()) {
                            List<Post> posts = response.body();
                            if (posts != null) {
                                feed.postValue(posts);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                        Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }
}
