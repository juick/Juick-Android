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
package com.juick.android.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juick.App
import com.juick.R
import com.juick.android.ui.screens.feed.buildUrlPositions
import com.juick.android.ui.screens.feed.formatPostText
import com.juick.api.model.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(
    uname: String,
    onUserClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    var messagesState by remember { mutableStateOf<Result<List<Post>>?>(null) }
    val messages = (messagesState?.getOrNull() ?: emptyList()).reversed()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uname) {
        try {
            messagesState = Result.success(withContext(Dispatchers.IO) { App.instance.api.pm(uname) })
        } catch (e: CancellationException) { throw e } catch (e: Exception) { messagesState = Result.failure(e) }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(uname) {
        App.instance.messages.collect { newMessages ->
            newMessages.filter { it.user.uname == uname || it.to?.uname == uname }.forEach { msg ->
                val current = messagesState?.getOrNull() ?: emptyList()
                messagesState = Result.success(current + msg)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
        ) {
            items(messages, key = { it.getTimestamp()?.time ?: it.hashCode() }) { post ->
                ChatBubble(
                    post = post,
                    isOwn = post.user.uname != uname,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(stringResource(R.string.Enter_a_message)) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (inputText.isNotBlank()) {
                    scope.launch {
                        try {
                            App.instance.api.postPm(uname, inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        } catch (_: Exception) { }
                    }
                }
            }) {
                Icon(Icons.Default.Send, stringResource(R.string.Send))
            }
        }
    }
}

@Composable
fun ChatBubble(
    post: Post,
    isOwn: Boolean,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val bgColor = colors.surfaceVariant
    val textColor = colors.onSurfaceVariant
    val alignment = if (isOwn) Alignment.End else Alignment.Start

    val (annotatedText, urlPositions) = remember(post) {
        formatPostText(post, colors.primary, colors.onSurfaceVariant, textColor) to buildUrlPositions(post)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = bgColor,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.padding(12.dp),
                onClick = { offset ->
                    urlPositions
                        .firstOrNull { it.start <= offset && offset < it.end }
                        ?.let { onLinkClick(it.url) }
                },
            )
        }
    }
}
