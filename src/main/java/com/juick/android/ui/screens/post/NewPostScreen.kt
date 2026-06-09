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
package com.juick.android.ui.screens.post

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juick.App
import com.juick.R
import com.juick.api.model.PostResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun NewPostScreen(
    initialText: String? = null,
    initialUri: String? = null,
    onTagsClick: () -> Unit,
    onAttachClick: () -> Unit,
    onAttachmentRemoved: () -> Unit,
    attachmentUri: Uri?,
    attachmentMime: String? = null,
    hasAttachment: Boolean,
    onNavigateToThread: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText ?: "") }
    val scope = rememberCoroutineScope()
    val messagePosted = remember { MutableStateFlow<Result<PostResponse>?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val sendEnabled = text.length >= 3 || attachmentUri != null

    LaunchedEffect(messagePosted) {
        messagePosted.collect { response ->
            if (response != null) {
                response.fold(
                    onSuccess = { postResponse ->
                        isSending = false
                        postResponse.newMessage?.let { post ->
                            onNavigateToThread(post.mid)
                        }
                    },
                    onFailure = {
                        isSending = false
                    },
                )
                messagePosted.value = null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
            Text(
                stringResource(R.string.New_message),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(64.dp)) // balance
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.Enter_a_message)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            minLines = 7,
        )

        if (isSending) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onTagsClick) {
                Text("#", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = {
                if (hasAttachment) {
                    onAttachmentRemoved()
                } else {
                    onAttachClick()
                }
            }) {
                Text("📎", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(
                onClick = {
                    if (sendEnabled && !isSending) {
                        isSending = true
                        scope.launch {
                            App.instance.sendMessage(scope, messagePosted, text, attachmentUri, attachmentMime)
                        }
                    }
                },
                enabled = sendEnabled && !isSending,
            ) {
                Text(stringResource(R.string.Send))
            }
        }
    }
}
