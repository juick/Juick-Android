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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.screens.chats.ChatsViewModel
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.screens.noauth.NoAuthScreen
import com.juick.api.model.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onChatClick: (Chat) -> Unit,
    onNavigateToAuth: () -> Unit,
    vm: ChatsViewModel = viewModel(),
) {
    val chatsState by vm.chats.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (App.instance.isAuthenticated) {
            vm.loadChats()
        } else {
            onNavigateToAuth()
        }
    }

    when (val result = chatsState) {
        null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = chat.dialogPhoto,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = chat.dialogName,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = chat.lastMessageOrNull?.getBody() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
