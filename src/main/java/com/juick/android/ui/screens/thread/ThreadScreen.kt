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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juick.App
import com.juick.R
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.JuickTheme
import com.juick.android.ui.screens.feed.PostCard
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
    var replyText by remember { mutableStateOf("") }
    var replyToPost by remember { mutableStateOf<Post?>(null) }
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
                    replyText = ""
                    replyToPost = null
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.ime)),
    ) {
        if (isLoading || isSending) LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (loadError) {
                Text(
                    stringResource(R.string.network_error),
                    color = colors.text,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(posts, key = { "thread_${it.mid}_${it.rid}" }) { post ->
                        PostCard(
                            post = post,
                            onPostClick = { replyToPost = post },
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
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Column(Modifier.fillMaxWidth().background(colors.mainBackground)) {
            val target = replyToPost
            if (target != null && target.rid > 0) {
                val inReplyTo = stringResource(R.string.In_reply_to_) + " "
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inReplyTo) }
                        append(target.getText())
                    },
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (replyAttachmentUri != null) { replyAttachmentUri = null; replyAttachmentMime = null }
                    else imagePicker.pick()
                }) {
                    Icon(
                        painterResource(R.drawable.ic_button_attachment),
                        stringResource(R.string.Attach),
                        tint = if (replyAttachmentUri != null) colors.accent else colors.text,
                    )
                }
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        cursorColor = colors.primary,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                    ),
                )
                val canSend = (replyText.isNotBlank() || replyAttachmentUri != null) && !isSending
                IconButton(
                    onClick = {
                        if (canSend && App.instance.isAuthenticated) {
                            val rid = replyToPost?.rid ?: 0
                            val to = if (rid > 0) "#$mid/$rid" else "#$mid"
                            isSending = true
                            try {
                                App.instance.sendMessage(scope, messagePosted, "$to $replyText", replyAttachmentUri, replyAttachmentMime)
                            } catch (e: FileNotFoundException) {
                                isSending = false
                                Toast.makeText(context, "Attachment error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSending,
                ) {
                    Icon(painterResource(R.drawable.ic_send_black_24dp), stringResource(R.string.Send), tint = colors.text)
                }
            }
        }
    }
}
