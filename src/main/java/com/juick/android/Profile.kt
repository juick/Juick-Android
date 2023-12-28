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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juick.App
import com.juick.api.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel: ViewModel() {
    val signInStatus = MutableLiveData(SignInActivity.SignInStatus.SIGNED_OUT)
    val anonymous = User(uid = 0, uname = "Anonymous")
    private val _userProfile = MutableLiveData<User?>(null)
    val userProfile: LiveData<User?> get() = _userProfile

    fun refresh() {
        _userProfile.postValue(null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _userProfile.postValue(App.instance.api.me())
            } catch (e: Exception) {
                _userProfile.postValue(anonymous)
            }
        }
    }
}