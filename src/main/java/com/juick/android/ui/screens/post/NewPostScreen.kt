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
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.juick.App
import com.juick.R
import com.juick.android.ui.widget.rememberImagePicker
import com.juick.api.model.PostResponse
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.FileNotFoundException

@Composable
fun NewPostScreen(
    initialText: String? = null,
    initialAttachment: Uri? = null,
    pendingTag: String? = null,
    onTagConsumed: () -> Unit = {},
    onTagsClick: () -> Unit,
    onNavigateToThread: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialText ?: "", TextRange((initialText?.length ?: 0)))) }
    val scope = rememberCoroutineScope()
    val messagePosted = remember { MutableStateFlow<Result<PostResponse>?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var attachmentUri by remember { mutableStateOf(initialAttachment) }
    var attachmentMime by remember { mutableStateOf(initialAttachment?.let { context.contentResolver.getType(it) }) }

    LaunchedEffect(pendingTag) {
        val tag = pendingTag ?: return@LaunchedEffect
        val text = textFieldValue.text
        val insert = if (text.isEmpty() || text.endsWith(' ') || text.endsWith('\n')) "#$tag " else " #$tag "
        val newText = text + insert
        textFieldValue = TextFieldValue(newText, TextRange(newText.length))
        onTagConsumed()
    }
    val sendEnabled = textFieldValue.text.length >= 3 || attachmentUri != null

    LaunchedEffect(messagePosted) {
        messagePosted.collect { response ->
            if (response != null) {
                response.fold(
                    onSuccess = { postResponse ->
                        isSending = false
                        postResponse.newMessage?.let { post -> onNavigateToThread(post.mid) }
                    },
                    onFailure = {
                        isSending = false
                        Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
                    },
                )
                messagePosted.value = null
            }
        }
    }

    val imagePicker = rememberImagePicker { croppedUri ->
        attachmentUri = croppedUri
        attachmentMime = "image/jpeg"
    }

    Composer(
        title = stringResource(R.string.New_message),
        text = textFieldValue,
        onTextChange = { textFieldValue = it },
        attachment = attachmentUri,
        onAttachClick = { imagePicker.pick() },
        onRemoveAttachment = { attachmentUri = null; attachmentMime = null },
        sendEnabled = sendEnabled,
        isSending = isSending,
        onSend = {
            isSending = true
            try {
                App.instance.sendMessage(scope, messagePosted, textFieldValue.text, attachmentUri, attachmentMime)
            } catch (e: FileNotFoundException) {
                isSending = false
                Toast.makeText(context, "Attachment error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        },
        onCancel = onCancel,
        onTagsClick = onTagsClick,
    )
}
