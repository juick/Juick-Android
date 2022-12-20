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

package com.juick.android.screens.pm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.juick.App
import com.juick.android.Resource
import com.juick.api.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PMViewModel(private val userName: String): ViewModel() {
    val messages = MutableStateFlow<Resource<List<Post>>>(Resource.loading())

    fun loadMessages() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { App.instance.api.pm(userName) }.let { newPms ->
                    messages.update {
                        Resource.success(newPms)
                    }
                }
            } catch (e: Exception) {
                messages.update {
                    Resource.error(null, e.message)
                }
            }
        }
    }
}

class PMViewModelFactory(private val userName: String): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(String::class.java)
            .newInstance(userName)
    }
}