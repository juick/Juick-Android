package com.juick.android.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Home : Route
    @Serializable data object Discover : Route
    @Serializable data object Chats : Route
    @Serializable data object Discussions : Route
    @Serializable data class Thread(val mid: Int, val scrollToEnd: Boolean = false) : Route
    @Serializable data class Blog(val uname: String) : Route
    @Serializable data class Chat(val uname: String, val uid: Int) : Route
    @Serializable data class Search(val query: String? = null) : Route
    @Serializable data object NewPost : Route
    @Serializable data class NewPostWithText(val text: String?) : Route
    @Serializable data object Tags : Route
    @Serializable data object NoAuth : Route
}
