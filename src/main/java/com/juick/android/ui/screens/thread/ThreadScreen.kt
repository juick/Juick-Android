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
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juick.App
import com.juick.R
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.JuickDivider
import com.juick.android.ui.JuickTheme
import com.juick.android.ui.screens.feed.PostCard
import com.juick.android.ui.screens.post.Composer
import com.juick.android.ui.widget.rememberImagePicker
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

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
    onPostDeleted: () -> Unit = onDismiss,
    currentUid: Int = 0,
    isPremiumOrAdmin: Boolean = false,
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var replyText by remember { mutableStateOf(TextFieldValue()) }
    var replyToPost by remember { mutableStateOf<Post?>(null) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var replyAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var replyAttachmentMime by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val colors = JuickTheme.colors
    val context = LocalContext.current

    val imagePicker = rememberImagePicker { croppedUri ->
        replyAttachmentUri = croppedUri
        replyAttachmentMime = "image/jpeg"
    }
    var subscribing by remember { mutableStateOf(false) }

    var reloadTrigger by remember { mutableIntStateOf(0) }
    val messagePosted = remember { MutableStateFlow<Result<PostResponse>?>(null) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(mid, reloadTrigger) {
        try { posts = App.instance.api.thread(mid); loadError = false } catch (e: CancellationException) { throw e } catch (_: Exception) { loadError = true }
        isLoading = false
        if ((scrollToEnd || reloadTrigger > 0) && posts.isNotEmpty()) listState.animateScrollToItem(posts.size - 1)
        posts.lastOrNull()?.let { try { App.instance.api.markRead(it.mid, it.rid) } catch (e: CancellationException) { throw e } catch (e: Exception) { Log.e("ThreadScreen", "markRead failed", e) } }
    }

    LaunchedEffect(messagePosted) {
        messagePosted.collect { response ->
            response ?: return@collect
            isSending = false
            response.fold(
                onSuccess = {
                    replyText = TextFieldValue()
                    replyToPost = null
                    composing = false
                    replyAttachmentUri = null
                    replyAttachmentMime = null
                    reloadTrigger++
                },
                onFailure = { Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show() },
            )
            messagePosted.value = null
        }
    }

    val newMessages by App.instance.messages.collectAsStateWithLifecycle()
    LaunchedEffect(newMessages) {
        val known = posts.mapTo(HashSet()) { it.rid }
        val relevant = newMessages.filter { it.mid == mid && it.rid > 0 && it.rid !in known }
        if (relevant.isNotEmpty()) posts = posts + relevant
    }

    LaunchedEffect(Unit) {
        App.instance.postUpdates.collect { updated ->
            posts = posts.map { post -> if (post.mid == updated.mid && post.rid == 0) updated else post }
        }
    }

    fun toggleSubscription(head: Post) {
        if (subscribing || !App.instance.isAuthenticated) return
        subscribing = true
        scope.launch {
            try {
                val updated = App.instance.api.subscribe(mid)
                posts = listOf(head.copy(subscribed = updated.subscribed)) + posts.drop(1)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
            } finally {
                subscribing = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.ime)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(colors.mainBackground).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.Cancel), tint = colors.primary)
            }
            Text(
                stringResource(R.string.Message),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
        }
        JuickDivider()
        if (isLoading || isSending) LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.border)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (loadError) {
                Text(
                    stringResource(R.string.network_error),
                    color = colors.text,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    itemsIndexed(posts, key = { _, it -> "thread_${it.mid}_${it.rid}" }) { index, post ->
                        PostCard(
                            post = post,
                            onPostClick = {
                                if (App.instance.isAuthenticated) {
                                    replyToPost = post
                                    composing = true
                                }
                            },
                            onReplyClick = {
                                if (App.instance.isAuthenticated) {
                                    replyToPost = post
                                    composing = true
                                }
                            },
                            onUserClick = { onUserClick(post.user.uname) },
                            onMenuClick = { onMenuClick(post) },
                            onLikeClick = { onLikeClick(post) },
                            onLinkClick = onLinkClick,
                            currentUid = currentUid,
                            isPremiumOrAdmin = isPremiumOrAdmin,
                            onDeletePost = { if (post.rid == 0) onPostDeleted() else reloadTrigger++ },
                            onSubscribeToggle = if (post.rid == 0) ({ toggleSubscription(post) }) else null,
                            onReplyToClick = { rid ->
                                val index = posts.indexOfFirst { it.rid == rid }
                                if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                            },
                            modifier = if (index == 1) Modifier.padding(top = 12.dp) else Modifier,
                            inThread = true,
                            panelTop = index == 1,
                            panelBottom = index == posts.lastIndex,
                        )
                    }
                }
            }
        }

    }

    AnimatedVisibility(
        visible = composing,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        BackHandler(enabled = !isSending) { composing = false }
        val target = replyToPost ?: posts.firstOrNull()
        Composer(
            title = stringResource(R.string.reply) + (target?.let { " @${it.user.uname}" } ?: ""),
            text = replyText,
            onTextChange = { replyText = it },
            attachment = replyAttachmentUri,
            onAttachClick = { imagePicker.pick() },
            onRemoveAttachment = { replyAttachmentUri = null; replyAttachmentMime = null },
            sendEnabled = replyText.text.isNotBlank() || replyAttachmentUri != null,
            isSending = isSending,
            onSend = {
                val rid = replyToPost?.rid ?: 0
                val to = if (rid > 0) "#$mid/$rid" else "#$mid"
                isSending = true
                try {
                    App.instance.sendMessage(scope, messagePosted, "$to ${replyText.text}", replyAttachmentUri, replyAttachmentMime)
                } catch (e: FileNotFoundException) {
                    isSending = false
                    Toast.makeText(context, "Attachment error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onCancel = { composing = false },
            replyTo = target,
        )
    }
    }
}
