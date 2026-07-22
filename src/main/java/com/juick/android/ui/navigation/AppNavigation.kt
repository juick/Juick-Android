package com.juick.android.ui.navigation

import androidx.compose.runtime.Composable
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
import com.juick.android.ui.AppScaffold
import com.juick.android.Uris
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
    onFabClick: () -> Unit,
    currentProfile: com.juick.api.model.User?,
    unreadCount: Int,
) {
    NavHost(navController = navController, startDestination = Route.Home) {
        composable<Route.Home> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.home, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick)
            }
        }
        composable<Route.Discover> {
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.last, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick)
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
                FeedScreen(Uris.discussions, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick)
            }
        }

        composable<Route.Thread> { entry ->
            val route = entry.toRoute<Route.Thread>()
            ThreadScreen(route.mid, route.scrollToEnd, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, onDismiss = { navController.popBackStack() })
        }

        composable<Route.Blog> { entry ->
            val uname = entry.toRoute<Route.Blog>().uname
            AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                FeedScreen(Uris.getUserPostsByName(uname), onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, showProfileHeader = true, profileHeader = { ProfileHeader(uname = uname) })
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
            val route = entry.toRoute<Route.Search>()
            route.query?.let {
                AppScaffold(navController, currentProfile, unreadCount, onSignInClick, onFabClick) {
                    FeedScreen(Uris.search(it), onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick)
                }
            }
        }

        composable<Route.NoAuth> {
            NoAuthScreen(onSignInClick = { navController.popBackStack(); onSignInClick() })
        }

        composable<Route.NewPost> {
            NewPostScreen(onTagsClick = { navController.navigate(Route.Tags) }, onAttachClick = {}, onAttachmentRemoved = {}, attachmentUri = null, hasAttachment = false, onNavigateToThread = { mid -> navController.popBackStack<Route.NewPost>(false); navController.navigate(Route.Thread(mid)) }, onDismiss = { navController.popBackStack() })
        }

        composable<Route.NewPostWithText> { entry ->
            val text = entry.toRoute<Route.NewPostWithText>().text
            NewPostScreen(initialText = text, onTagsClick = { navController.navigate(Route.Tags) }, onAttachClick = {}, onAttachmentRemoved = {}, attachmentUri = null, hasAttachment = false, onNavigateToThread = { mid -> navController.popBackStack<Route.NewPost>(false); navController.navigate(Route.Thread(mid)) }, onDismiss = { navController.popBackStack() })
        }

        dialog<Route.Tags> {
            TagsScreen(onTagSelected = { tag -> navController.previousBackStackEntry?.savedStateHandle?.set("tag", tag); navController.popBackStack() })
        }
    }
}
