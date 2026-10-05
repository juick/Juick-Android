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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.ui.JuickTheme
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

    val colors = JuickTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom)),
    ) {
        if (isSending) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(Modifier.weight(1f).fillMaxWidth()) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp).focusRequester(focusRequester),
                minLines = 7,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    cursorColor = colors.primary,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                ),
            )
            attachmentUri?.let {
                AsyncImage(
                    it, stringResource(R.string.Attach),
                    Modifier.fillMaxWidth().weight(1f),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(colors.mainBackground).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onTagsClick) {
                Icon(painterResource(R.drawable.ic_code_tags_black_24dp), stringResource(R.string.tags_button), tint = colors.text)
            }
            Spacer(Modifier.width(5.dp))
            IconButton(onClick = {
                if (attachmentUri != null) { attachmentUri = null; attachmentMime = null }
                else imagePicker.pick()
            }) {
                Icon(
                    painterResource(
                        if (attachmentUri != null) R.drawable.ic_attach_file_black_24dp_on
                        else R.drawable.ic_attach_file_black_24dp
                    ),
                    stringResource(R.string.attach_photo),
                    tint = if (attachmentUri != null) colors.accent else colors.text,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (sendEnabled && !isSending) {
                        isSending = true
                        try {
                            App.instance.sendMessage(scope, messagePosted, textFieldValue.text, attachmentUri, attachmentMime)
                        } catch (e: FileNotFoundException) {
                            isSending = false
                            Toast.makeText(context, "Attachment error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSending,
            ) { Icon(painterResource(R.drawable.ic_send_black_24dp), stringResource(R.string.send_button), tint = colors.text) }
        }
    }
}
