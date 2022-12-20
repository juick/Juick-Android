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

package com.juick.android.screens.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juick.App
import com.juick.android.Resource
import com.juick.api.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagsViewModel : ViewModel() {
    private val _tags = MutableStateFlow<Resource<List<Tag>>>(Resource.loading())
    val tags = _tags.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    App.instance.api.tags()
                }
                _tags.update {
                    Resource.success(data = data)
                }
            } catch (exception: Exception) {
                _tags.update {
                    Resource.error(
                        data = null,
                        message = exception.message
                    )
                }
            }
        }
    }
}