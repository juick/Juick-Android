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
package com.juick.android.screens.chats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.juick.App
import com.juick.android.Resource
import com.juick.android.Utils
import kotlinx.coroutines.Dispatchers

class ChatsViewModel : ViewModel() {
    var chats = liveData(Dispatchers.IO) {
        emit(Resource.loading(null))
        try {
            val pms = App.instance.api?.groupsPms(10)?.pms ?: listOf()
            emit(Resource.success(data = pms))
        } catch (exception: Exception) {
            emit(Resource.error(data = null, message = exception.message ?: "Error Occurred!"))
        }
    }

    private val authenticated = MutableLiveData(Utils.hasAuth())
    fun isAuthenticated(): LiveData<Boolean> {
        return authenticated
    }
}