/*
 * Copyright (C) 2008-2023, Juick
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

import androidx.lifecycle.ViewModel
import com.juick.App
import com.juick.api.model.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class ChatsViewModel : ViewModel() {
    var chats = MutableStateFlow<Result<List<Chat>>?>(null)
        private set

    suspend fun loadChats() {
        chats.value = null
        try {
            val pms = withContext(Dispatchers.IO) {
                App.instance.api.groupsPms(10).pms
            }
            chats.value = Result.success(pms)
        } catch (exception: Exception) {
            chats.value =
                Result.failure(exception)
        }
    }
}