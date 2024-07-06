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
package com.juick.android.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juick.App
import com.juick.api.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class FeedViewModel(val state: SavedStateHandle) : ViewModel() {
    val apiUrl: MutableStateFlow<String> = MutableStateFlow("")
    private val _feed: MutableStateFlow<Result<List<Post>>?> = MutableStateFlow(null)
    val feed = _feed.asStateFlow()

    init {
        viewModelScope.launch {
            apiUrl.collect { url ->
                if (url.isNotEmpty()) {
                    _feed.update {
                        null
                    }
                    _feed.update {
                        runCatching {
                            val posts = withContext(Dispatchers.IO) {
                                App.instance.api.getPosts(url)
                            }
                            return@runCatching posts
                        }
                    }
                }
            }
        }
    }
    fun feedReceived() {
        _feed.update { Result.success(listOf()) }
    }
}