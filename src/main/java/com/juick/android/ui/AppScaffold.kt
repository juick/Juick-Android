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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.juick.R
import com.juick.android.ui.navigation.Route
import com.juick.api.model.User

enum class BottomTab { Home, Discover, Chats }

@Composable
fun JuickDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(JuickTheme.colors.border))
}

@Composable
fun JuickTopBar(
    title: String,
    navController: NavHostController,
    currentProfile: User?,
    unreadCount: Int,
    onBack: (() -> Unit)? = null,
) {
    val colors = JuickTheme.colors
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val submit = {
        if (query.isNotBlank()) {
            searching = false
            navController.navigate(Route.Search(query.trim()))
            query = ""
        }
    }

    Column(Modifier.background(colors.textBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backAction = if (searching) ({ searching = false; query = "" }) else onBack
            if (backAction != null) {
                IconButton(onClick = backAction) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.Cancel), tint = colors.text)
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            if (searching) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                Row(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .drawBehind { drawRect(colors.darkerGray, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx())) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_ei_search), null, Modifier.size(24.dp), tint = colors.darkerGray)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                        cursorBrush = SolidColor(colors.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() }),
                        modifier = Modifier.weight(1f).padding(start = 8.dp).focusRequester(focusRequester),
                    )
                    IconButton(onClick = submit) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.search), tint = colors.text)
                    }
                }
            } else {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = if (onBack != null) 8.dp else 0.dp),
                )
                IconButton(onClick = { searching = true }) {
                    Icon(painterResource(R.drawable.ic_ei_search), stringResource(R.string.search), Modifier.size(28.dp), tint = colors.text)
                }
            }
            IconButton(onClick = { navController.navigate(Route.Discussions) { launchSingleTop = true } }) {
                BadgedBox(badge = {
                    if (unreadCount > 0) Badge(containerColor = colors.accent, contentColor = colors.mainBackground) { Text("$unreadCount") }
                }) {
                    Icon(painterResource(R.drawable.ic_ei_bell), stringResource(R.string.Discussions), Modifier.size(28.dp), tint = colors.text)
                }
            }
            if (currentProfile != null && currentProfile.uid > 0) {
                Box(
                    Modifier.size(52.dp).clickable { navController.navigate(Route.Blog(currentProfile.uname)) { launchSingleTop = true } },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = currentProfile.avatar,
                        contentDescription = stringResource(R.string.Me),
                        placeholder = painterResource(R.drawable.av_96),
                        error = painterResource(R.drawable.av_96),
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        }
        JuickDivider()
    }
}

@Composable
fun JuickBottomBar(navController: NavHostController, selected: BottomTab) {
    val colors = JuickTheme.colors
    Column(Modifier.background(colors.mainBackground)) {
        JuickDivider()
        NavigationBar(containerColor = colors.mainBackground, tonalElevation = 0.dp) {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = colors.accent,
                unselectedIconColor = colors.dimmed,
            )
            fun go(route: Route) = navController.navigate(route) {
                popUpTo(Route.Home) { inclusive = route == Route.Home }
                launchSingleTop = true
            }
            NavigationBarItem(
                selected = selected == BottomTab.Home,
                onClick = { go(Route.Home) },
                icon = { Icon(painterResource(R.drawable.ic_ei_clock), stringResource(R.string.Subscriptions)) },
                colors = navColors,
            )
            NavigationBarItem(
                selected = selected == BottomTab.Discover,
                onClick = { go(Route.Discover) },
                icon = { Icon(painterResource(R.drawable.icon_discover), stringResource(R.string.Discover)) },
                colors = navColors,
            )
            NavigationBarItem(
                selected = selected == BottomTab.Chats,
                onClick = { go(Route.Chats) },
                icon = { Icon(painterResource(R.drawable.ic_ei_envelope), stringResource(R.string.PMs)) },
                colors = navColors,
            )
        }
    }
}

@Composable
private fun selectedTab(navController: NavHostController): BottomTab {
    val entry by navController.currentBackStackEntryAsState()
    val stack = navController.currentBackStack.value
    val names = stack.mapNotNull { it.destination.route?.substringBefore('/')?.substringBefore('?') }
    val last = (names + listOfNotNull(entry?.destination?.route)).lastOrNull { name ->
        name == Route.Home::class.qualifiedName || name == Route.Discover::class.qualifiedName || name == Route.Chats::class.qualifiedName
    }
    return when (last) {
        Route.Discover::class.qualifiedName -> BottomTab.Discover
        Route.Chats::class.qualifiedName -> BottomTab.Chats
        else -> BottomTab.Home
    }
}

@Composable
fun AppScaffold(
    navController: NavHostController,
    currentProfile: User?,
    unreadCount: Int,
    title: String,
    onFabClick: () -> Unit,
    showBack: Boolean = false,
    showBottomBar: Boolean = true,
    showFab: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = JuickTheme.colors
    val tab = selectedTab(navController)
    Scaffold(
        containerColor = colors.mainBackground,
        topBar = {
            Box(Modifier.background(colors.textBackground).statusBarsPadding()) {
                JuickTopBar(title, navController, currentProfile, unreadCount, onBack = if (showBack) ({ navController.popBackStack() }) else null)
            }
        },
        bottomBar = {
            if (showBottomBar) JuickBottomBar(navController, tab)
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = onFabClick, containerColor = colors.accent, contentColor = colors.mainBackground) {
                    Icon(painterResource(R.drawable.ic_ei_pencil), contentDescription = stringResource(R.string.Create), modifier = Modifier.size(24.dp))
                }
            }
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) { content() }
    }
}
