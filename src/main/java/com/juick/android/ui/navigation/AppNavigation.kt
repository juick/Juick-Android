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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.juick.android.Uris
import com.juick.android.ui.screens.chats.ChatsListScreen
import com.juick.android.ui.screens.feed.FeedScreen
import com.juick.api.model.Post

@Composable
fun AppNavigation(
    navController: NavHostController,
    onPostClick: (Post) -> Unit,
    onUserClick: (String) -> Unit,
    onMenuClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit,
    onLinkClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onTagSelected: (String) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    when (route) {
        "home" -> FeedScreen(
            initialUrl = Uris.home,
            onPostClick = { onPostClick(it) },
            onUserClick = { onUserClick(it) },
            onMenuClick = { onMenuClick(it) },
            onLikeClick = { onLikeClick(it) },
            onLinkClick = { onLinkClick(it) },
        )
        "discover" -> FeedScreen(
            initialUrl = Uris.last,
            onPostClick = { onPostClick(it) },
            onUserClick = { onUserClick(it) },
            onMenuClick = { onMenuClick(it) },
            onLikeClick = { onLikeClick(it) },
            onLinkClick = { onLinkClick(it) },
        )
        "chats" -> ChatsListScreen(
            onChatClick = { chat ->
                navController.navigate("chat/${chat.dialogName}/${chat.uid}")
            },
            onNavigateToAuth = { navController.navigate("no_auth") },
        )
        else -> {}
    }
}
