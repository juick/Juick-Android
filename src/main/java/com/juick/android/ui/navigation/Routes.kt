/*
 * Copyright (C) 2008-2026, Juick
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
package com.juick.android.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Public : Route
    @Serializable data object Home : Route
    @Serializable data object Discover : Route
    @Serializable data object Chats : Route
    @Serializable data object Discussions : Route
    @Serializable data class Thread(val mid: Int, val scrollToEnd: Boolean = false) : Route
    @Serializable data class Blog(val uname: String) : Route
    @Serializable data class Chat(val uname: String, val uid: Int) : Route
    @Serializable data class Search(val query: String? = null) : Route
    @Serializable data class NewPost(val text: String? = null) : Route
    @Serializable data object Tags : Route
    @Serializable data object NoAuth : Route
}
