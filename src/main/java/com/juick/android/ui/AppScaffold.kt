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
package com.juick.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.juick.R
import com.juick.android.ui.navigation.Route
import com.juick.api.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    currentProfile: User?,
    unreadCount: Int,
    onSignInClick: () -> Unit,
    onFabClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val borderLine = @Composable { Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline)) }

    val bottomNavVisible = route in listOf(
        Route.Home::class.qualifiedName,
        "com.juick.android.ui.navigation.Route.Discover",
        "com.juick.android.ui.navigation.Route.Chats",
        "com.juick.android.ui.navigation.Route.Discussions",
        "com.juick.android.ui.navigation.Route.Blog",
        "com.juick.android.ui.navigation.Route.Search",
    )

    val fabVisible = route in listOf(
        Route.Home::class.qualifiedName,
        "com.juick.android.ui.navigation.Route.Discover",
    )

    val screenTitle = when (route) {
        Route.Discover::class.qualifiedName -> stringResource(R.string.Discover)
        Route.Chats::class.qualifiedName -> stringResource(R.string.PMs)
        Route.Discussions::class.qualifiedName -> "Discussions"
        else -> stringResource(R.string.Juick)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(screenTitle) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface),
                    actions = {
                        IconButton(onClick = { navController.navigate(Route.Search(null)) { launchSingleTop = true } }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { navController.navigate(Route.Discussions) { launchSingleTop = true } }) {
                            BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Discussions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (currentProfile != null) {
                            IconButton(onClick = { navController.navigate(Route.Blog(currentProfile.uname)) { launchSingleTop = true } }) {
                                AsyncImage(model = currentProfile.avatar, contentDescription = stringResource(R.string.Me), modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            }
                        }
                    },
                )
                borderLine()
            }
        },
        bottomBar = {
            if (bottomNavVisible) {
                Column {
                    borderLine()
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        val navColors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onSurface, selectedTextColor = MaterialTheme.colorScheme.onSurface, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.tertiary)
                        NavigationBarItem(selected = route == Route.Home::class.qualifiedName, onClick = { navController.navigate(Route.Home) { popUpTo(Route.Home) { inclusive = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(painterResource(R.drawable.ic_ei_clock), contentDescription = null) }, label = { Text(stringResource(R.string.Subscriptions)) }, colors = navColors)
                        NavigationBarItem(selected = route == Route.Discover::class.qualifiedName, onClick = { navController.navigate(Route.Discover) { popUpTo(Route.Home) { inclusive = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(painterResource(R.drawable.icon_discover), contentDescription = null) }, label = { Text(stringResource(R.string.Discover)) }, colors = navColors)
                        NavigationBarItem(selected = route == Route.Chats::class.qualifiedName, onClick = { navController.navigate(Route.Chats) { popUpTo(Route.Home) { inclusive = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(painterResource(R.drawable.ic_ei_envelope), contentDescription = null) }, label = { Text(stringResource(R.string.PMs)) }, colors = navColors)
                    }
                }
            }
        },
        floatingActionButton = {
            if (fabVisible) {
                FloatingActionButton(onClick = onFabClick, containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) {
                    Icon(painterResource(R.drawable.ic_ei_pencil), contentDescription = stringResource(R.string.Create), modifier = Modifier.size(24.dp))
                }
            }
        },
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding)) { content() }
    }
}
