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
package com.juick.android.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.juick.android.ui.JuickTheme
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.service.isAuthenticated
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.juick.android.ui.screens.noauth.NoAuthScreen
import com.juick.api.model.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onChatClick: (Chat) -> Unit,
    onNavigateToAuth: () -> Unit,
) {
    var chatsState by remember { mutableStateOf<Result<List<Chat>>?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit, refreshTrigger) {
        if (App.instance.isAuthenticated) {
            try {
                chatsState = Result.success(withContext(Dispatchers.IO) { App.instance.api.groupsPms(10).pms })
            } catch (e: CancellationException) { throw e } catch (e: Exception) { chatsState = Result.failure(e) }
            isRefreshing = false
        } else {
            onNavigateToAuth()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; refreshTrigger++ },
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val result = chatsState) {
            null -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            else -> {
            result.fold(
                onSuccess = { chats ->
                    if (chats.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.you_have_no_direct_messages))
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(chats, key = { it.uid }) { chat ->
                                ChatListItem(
                                    chat = chat,
                                    onClick = { onChatClick(chat) },
                                )
                            }
                        }
                    }
                },
                onFailure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = it.message ?: stringResource(R.string.Error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    val colors = JuickTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        AsyncImage(
            model = chat.dialogPhoto,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(30)),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f)) {
            Row(Modifier.padding(top = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.dialogName,
                    fontSize = 16.sp,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                chat.getLastMessage().getTimestamp()?.let {
                    Text(timeFormat.format(it), fontSize = 16.sp, color = colors.darkerGray)
                }
            }
            Text(
                text = chat.getLastMessage().getBody() ?: "",
                fontSize = 16.sp,
                color = colors.darkerGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, end = 16.dp, bottom = 16.dp),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E0E0)))
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
