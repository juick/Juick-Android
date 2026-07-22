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
package com.juick.android.ui.screens.thread

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juick.App
import com.juick.R
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.screens.feed.PostCard
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    mid: Int,
    scrollToEnd: Boolean = false,
    onPostClick: (Post) -> Unit,
    onUserClick: (String) -> Unit,
    onMenuClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit,
    onLinkClick: (String) -> Unit,
    onDismiss: () -> Unit,
    currentUid: Int = 0,
    isPremiumOrAdmin: Boolean = false,
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var replyText by remember { mutableStateOf("") }
    var replyToPost by remember { mutableStateOf<Post?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var replyAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var replyAttachmentMime by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { replyAttachmentUri = it; replyAttachmentMime = "image/jpeg" }
    }

    LaunchedEffect(mid) {
        try { posts = App.instance.api.thread(mid) } catch (_: Exception) { loadError = true }
        isLoading = false
        if (scrollToEnd && posts.isNotEmpty()) listState.animateScrollToItem(posts.size - 1)
        posts.lastOrNull()?.let { App.instance.api.markRead(it.mid, it.rid) }
    }

    val newMessages by App.instance.messages.collectAsStateWithLifecycle()
    LaunchedEffect(newMessages) {
        val relevant = newMessages.filter { it.mid == mid }
        if (relevant.isNotEmpty()) posts = posts + relevant
    }

    Surface(color = colors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Thread") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.Cancel))
                    }
                },
            )
            if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (loadError) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.network_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(posts, key = { "thread_${it.mid}_${it.rid}" }) { post ->
                        PostCard(
                            post = post,
                            onPostClick = { replyToPost = post },
                            onUserClick = { onUserClick(post.user.uname) },
                            onMenuClick = { onMenuClick(post) },
                            onLikeClick = { onLikeClick(post) },
                            onLinkClick = onLinkClick,
                            showCounters = false,
                            currentUid = currentUid,
                            isPremiumOrAdmin = isPremiumOrAdmin,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Surface(color = colors.surface, shadowElevation = 2.dp) {
                replyToPost?.let { target ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = target.user.avatar, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(8.dp))
                        Text("In reply to ${target.user.uname}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { replyToPost = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, "Clear", tint = colors.onSurfaceVariant)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).imePadding(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text(stringResource(R.string.reply)) },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        if (replyAttachmentUri != null) { replyAttachmentUri = null; replyAttachmentMime = null }
                        else galleryLauncher.launch("image/*")
                    }) {
                        Text(if (replyAttachmentUri != null) "📎✓" else "📎", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if ((replyText.isNotBlank() || replyAttachmentUri != null) && App.instance.isAuthenticated) {
                                scope.launch {
                                    try {
                                        val receiver = MutableStateFlow<Result<PostResponse>?>(null)
                                        App.instance.sendMessage(scope, receiver, "#$mid $replyText", replyAttachmentUri, replyAttachmentMime)
                                        replyText = ""
                                        replyAttachmentUri = null
                                        replyAttachmentMime = null
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                        enabled = replyText.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Send, stringResource(R.string.Send), tint = if (replyText.isNotBlank()) colors.primary else colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
