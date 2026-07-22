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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juick.R
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.juick.android.ui.screens.chat.ChatScreen
import com.juick.android.ui.screens.chats.ChatsListScreen
import com.juick.android.ui.screens.feed.FeedScreen
import com.juick.android.ui.screens.feed.ProfileHeader
import com.juick.android.ui.screens.noauth.NoAuthScreen
import com.juick.android.ui.screens.post.NewPostScreen
import com.juick.android.ui.screens.search.SearchScreen
import com.juick.android.ui.screens.tags.TagsScreen
import com.juick.android.ui.screens.thread.ThreadScreen
import androidx.compose.ui.window.DialogProperties
import com.juick.android.ui.AppScaffold
import com.juick.android.Uris
import com.juick.api.model.Post

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    onPostClick: (Post) -> Unit,
    onUserClick: (String) -> Unit,
    onMenuClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit,
    onLinkClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onFabClick: () -> Unit,
    currentProfile: com.juick.api.model.User?,
    unreadCount: Int,
    isAuthenticated: Boolean,
) {
    var pendingTag by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate(Route.Home) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = if (isAuthenticated) Route.Home else Route.Public) {
        composable<Route.Public> {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.Juick)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    actions = {
                        TextButton(onClick = onSignInClick) { Text(stringResource(R.string.login)) }
                    },
                )
                Box(Modifier.fillMaxSize().padding()) {
                    FeedScreen(Uris.top, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, currentUser = currentProfile)
                }
            }
        }
        composable<Route.Home> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.home, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, currentUser = currentProfile)
            }
        }
        composable<Route.Discover> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.last, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, currentUser = currentProfile)
            }
        }
        composable<Route.Chats> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                ChatsListScreen(
                    onChatClick = { chat -> navController.navigate(Route.Chat(chat.dialogName, chat.uid)) },
                    onNavigateToAuth = { navController.navigate(Route.NoAuth) },
                )
            }
        }
        composable<Route.Discussions> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.discussions, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, currentUser = currentProfile)
            }
        }

        dialog<Route.Thread>(
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) { entry ->
            val route = entry.toRoute<Route.Thread>()
            ThreadScreen(route.mid, route.scrollToEnd, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, onDismiss = { navController.popBackStack() }, currentUid = currentProfile?.uid ?: 0, isPremiumOrAdmin = currentProfile?.premium == true || currentProfile?.admin == true)
        }

        composable<Route.Blog> { entry ->
            val uname = entry.toRoute<Route.Blog>().uname
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.getUserPostsByName(uname), onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, currentUser = currentProfile, showProfileHeader = true, profileHeader = { ProfileHeader(uname = uname) })
            }
        }

        composable<Route.Chat> { entry ->
            val route = entry.toRoute<Route.Chat>()
            ChatScreen(route.uname, onUserClick, onLinkClick)
        }

        composable<Route.Search> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                SearchScreen(onSearch = { query -> navController.navigate(Route.Search(query)) { popUpTo<Route.Search> { inclusive = true } } })
            }
        }

        composable<Route.Search> { entry ->
            val query = entry.toRoute<Route.Search>().query
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                if (query != null) FeedScreen(Uris.search(query), onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, currentUser = currentProfile)
                else SearchScreen(onSearch = { q -> navController.navigate(Route.Search(q)) { popUpTo<Route.Search> { inclusive = true } } })
            }
        }

        composable<Route.NoAuth> {
            NoAuthScreen(onSignInClick = { navController.popBackStack(); onSignInClick() })
        }

        composable<Route.NewPost> { entry ->
            val text = entry.toRoute<Route.NewPost>().text ?: ""
            var initialText by remember { mutableStateOf(text) }
            LaunchedEffect(pendingTag) {
                pendingTag?.let { tag ->
                    initialText = if (initialText.isNotEmpty()) "$initialText #$tag " else "#$tag "
                    pendingTag = null
                }
            }
            NewPostScreen(initialText = initialText, onTagsClick = { navController.navigate(Route.Tags) }, onNavigateToThread = { mid -> navController.popBackStack<Route.NewPost>(false); navController.navigate(Route.Thread(mid)) }, onDismiss = { navController.popBackStack() })
        }

        dialog<Route.Tags> {
            TagsScreen(onTagSelected = { tag -> pendingTag = tag; navController.popBackStack() })
        }
    }
}
