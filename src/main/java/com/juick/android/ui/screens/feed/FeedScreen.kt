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
package com.juick.android.ui.screens.feed

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juick.android.Utils
import com.juick.android.Utils.replaceUriParameter
import com.juick.android.screens.FeedViewModel
import com.juick.api.model.Post
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    initialUrl: Uri,
    onPostClick: (Post) -> Unit,
    onUserClick: (String) -> Unit,
    onMenuClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit,
    onLinkClick: (String) -> Unit,
    showProfileHeader: Boolean = false,
    profileHeader: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    vm: FeedViewModel = viewModel(),
) {
    val feedState by vm.feed.collectAsState()
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    var firstPage by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    
    LaunchedEffect(initialUrl) {
        if (vm.apiUrl.value == Uri.EMPTY) {
            vm.apiUrl.value = initialUrl
        }
    }

    
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && feedState != null) {
            val posts = feedState!!.getOrNull() ?: return@LaunchedEffect
            if (posts.isNotEmpty()) {
                val lastPost = posts.last()
                val oldBeforeMid = vm.apiUrl.value.getQueryParameter("before_mid")
                if ("${lastPost.mid}" != oldBeforeMid) {
                    val newUrl = Utils.buildUrl(vm.apiUrl.value)
                        .build()
                        .replaceUriParameter("before_mid", lastPost.mid.toString())
                        .replaceUriParameter("ts", "${System.currentTimeMillis()}")
                    firstPage = false
                    vm.apiUrl.value = newUrl
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            firstPage = true
            val newUrl = Utils.buildUrl(initialUrl)
                .build()
                .replaceUriParameter("before_mid", "")
                .replaceUriParameter("ts", "${System.currentTimeMillis()}")
            vm.apiUrl.value = newUrl
            scope.launch {
                snapshotFlow { feedState }
                    .distinctUntilChanged()
                    .collectLatest { result ->
                        if (result != null) {
                            isRefreshing = false
                        }
                    }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        when (val result = feedState) {
            null -> {
                if (firstPage) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            else -> {
                result.fold(
                    onSuccess = { posts ->
                        LazyColumn(state = listState) {
                            if (showProfileHeader) {
                                item(key = "profile_header") {
                                    profileHeader()
                                }
                            }
                            items(
                                items = posts,
                                key = { post -> "post_${post.mid}_${post.rid}" },
                            ) { post ->
                                if (post.rid == 0) {
                                    PostCard(
                                        post = post,
                                        onPostClick = { onPostClick(post) },
                                        onUserClick = { onUserClick(post.user.uname) },
                                        onMenuClick = { onMenuClick(post) },
                                        onLikeClick = { onLikeClick(post) },
                                        onLinkClick = onLinkClick,
                                        modifier = Modifier.padding(vertical = 4.dp).padding(horizontal = 12.dp),
                                    )
                                } else {
                                    ReplyCard(
                                        post = post,
                                        onPostClick = { onPostClick(post) },
                                        onUserClick = { onUserClick(post.user.uname) },
                                        onMenuClick = { onMenuClick(post) },
                                        onLikeClick = { onLikeClick(post) },
                                        onLinkClick = onLinkClick,
                                        modifier = Modifier.padding(vertical = 4.dp).padding(horizontal = 12.dp),
                                    )
                                }
                            }
                        }
                    },
                    onFailure = {
                        if (firstPage) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = it.message ?: "Error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun ReplyCard(
    post: Post,
    onPostClick: () -> Unit,
    onUserClick: () -> Unit,
    onMenuClick: () -> Unit,
    onLikeClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    
    
    Column(modifier = modifier) {
        val replyTo = post.to
        if (replyTo != null) {
            Text(
                text = "↳ ${replyTo.uname}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        PostCard(
            post = post,
            onPostClick = onPostClick,
            onUserClick = onUserClick,
            onMenuClick = onMenuClick,
            onLikeClick = onLikeClick,
            onLinkClick = onLinkClick,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
