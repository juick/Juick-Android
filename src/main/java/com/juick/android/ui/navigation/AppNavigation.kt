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

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.juick.android.Uris
import com.juick.android.ui.screens.chat.ChatScreen
import com.juick.android.ui.screens.chats.ChatsListScreen
import com.juick.android.ui.screens.feed.FeedScreen
import com.juick.android.ui.screens.feed.ProfileHeader
import com.juick.android.ui.screens.noauth.NoAuthScreen
import com.juick.android.ui.screens.post.NewPostScreen
import com.juick.android.ui.screens.search.SearchScreen
import com.juick.android.ui.screens.tags.TagsScreen
import com.juick.android.ui.screens.thread.ThreadScreen
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
    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            FeedScreen(
                initialUrl = Uris.home,
                onPostClick = { onPostClick(it) },
                onUserClick = { onUserClick(it) },
                onMenuClick = { onMenuClick(it) },
                onLikeClick = { onLikeClick(it) },
                onLinkClick = { onLinkClick(it) },
            )
        }

        composable("discover") {
            FeedScreen(
                initialUrl = Uris.last,
                onPostClick = { onPostClick(it) },
                onUserClick = { onUserClick(it) },
                onMenuClick = { onMenuClick(it) },
                onLikeClick = { onLikeClick(it) },
                onLinkClick = { onLinkClick(it) },
            )
        }

        composable("chats") {
            ChatsListScreen(
                onChatClick = { chat ->
                    navController.navigate("chat/${chat.dialogName}/${chat.uid}")
                },
                onNavigateToAuth = { navController.navigate("no_auth") },
            )
        }

        composable(
            "chat/{uname}/{uid}",
            arguments = listOf(
                navArgument("uname") { type = NavType.StringType },
                navArgument("uid") { type = NavType.IntType },
            ),
        ) { entry ->
            val uname = entry.arguments?.getString("uname") ?: ""
            ChatScreen(
                uname = uname,
                onUserClick = { onUserClick(it) },
                onLinkClick = { onLinkClick(it) },
            )
        }

        composable("blog/{uname}",
            arguments = listOf(navArgument("uname") { type = NavType.StringType }),
        ) { entry ->
            val uname = entry.arguments?.getString("uname") ?: ""
            FeedScreen(
                initialUrl = Uris.getUserPostsByName(uname),
                onPostClick = { onPostClick(it) },
                onUserClick = { onUserClick(it) },
                onMenuClick = { onMenuClick(it) },
                onLikeClick = { onLikeClick(it) },
                onLinkClick = { onLinkClick(it) },
                showProfileHeader = true,
                profileHeader = {
                    ProfileHeader(uname = uname)
                },
            )
        }

        composable("search") {
            SearchScreen(
                onSearch = { query ->
                    navController.navigate("search/${Uri.encode(query)}") {
                        popUpTo("search") { inclusive = true }
                    }
                },
            )
        }

        composable("search/{query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType }),
        ) { entry ->
            val query = entry.arguments?.getString("query") ?: ""
            FeedScreen(
                initialUrl = Uris.search(query),
                onPostClick = { onPostClick(it) },
                onUserClick = { onUserClick(it) },
                onMenuClick = { onMenuClick(it) },
                onLikeClick = { onLikeClick(it) },
                onLinkClick = { onLinkClick(it) },
            )
        }

        composable("discussions") {
            FeedScreen(
                initialUrl = Uris.discussions,
                onPostClick = { onPostClick(it) },
                onUserClick = { onUserClick(it) },
                onMenuClick = { onMenuClick(it) },
                onLikeClick = { onLikeClick(it) },
                onLinkClick = { onLinkClick(it) },
            )
        }

        composable("no_auth") {
            NoAuthScreen(onSignInClick = {
                navController.popBackStack()
                onSignInClick()
            })
        }

        dialog("thread/{mid}?scrollToEnd={scrollToEnd}",
            arguments = listOf(
                navArgument("mid") { type = NavType.IntType },
                navArgument("scrollToEnd") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val mid = entry.arguments?.getInt("mid") ?: 0
            val scrollToEnd = entry.arguments?.getBoolean("scrollToEnd") ?: false
            ThreadScreen(
                mid = mid,
                scrollToEnd = scrollToEnd,
                onPostClick = { onPostClick(it) },
                onUserClick = { onUserClick(it) },
                onMenuClick = { onMenuClick(it) },
                onLikeClick = { onLikeClick(it) },
                onLinkClick = { onLinkClick(it) },
                onDismiss = { navController.popBackStack() },
            )
        }

        dialog("tags") {
            TagsScreen(
                onTagSelected = { tag ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("tag", tag)
                    navController.popBackStack()
                },
            )
        }

        composable(
            "new_post?text={text}&uri={uri}",
            arguments = listOf(
                navArgument("text") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            NewPostScreen(
                initialText = entry.arguments?.getString("text"),
                initialUri = entry.arguments?.getString("uri"),
                onTagsClick = { navController.navigate("tags") },
                onAttachClick = { /* handled by MainActivity */ },
                onAttachmentRemoved = { },
                attachmentUri = null,
                hasAttachment = false,
                onNavigateToThread = { mid ->
                    navController.popBackStack("new_post", true)
                    navController.navigate("thread/$mid")
                },
                onDismiss = { navController.popBackStack() },
            )
        }
    }
}
