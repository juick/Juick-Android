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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.juick.App
import com.juick.R
import com.juick.android.ui.widget.CropSheet
import com.juick.api.model.PostResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun NewPostScreen(
    initialText: String? = null,
    onTagsClick: () -> Unit,
    onNavigateToThread: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var textFieldValue by remember(initialText) { mutableStateOf(TextFieldValue(initialText ?: "", TextRange((initialText?.length ?: 0)))) }
    val scope = rememberCoroutineScope()
    val messagePosted = remember { MutableStateFlow<Result<PostResponse>?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentMime by remember { mutableStateOf<String?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showCrop by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val sendEnabled = textFieldValue.text.length >= 3 || attachmentUri != null

    LaunchedEffect(messagePosted) {
        messagePosted.collect { response ->
            if (response != null) {
                response.fold(
                    onSuccess = { postResponse ->
                        isSending = false
                        postResponse.newMessage?.let { post -> onNavigateToThread(post.mid) }
                    },
                    onFailure = { isSending = false },
                )
                messagePosted.value = null
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showCrop = it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { showCrop = it }
    }

    fun launchCamera() {
        val file = File(context.filesDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    showCrop?.let { sourceUri ->
        CropSheet(
            imageUri = sourceUri,
            onCropResult = { croppedUri ->
                showCrop = null
                if (croppedUri != null) {
                    attachmentUri = croppedUri
                    attachmentMime = "image/jpeg"
                }
            },
            onDismiss = { showCrop = null },
        )
    }

    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("") },
            text = {
                Column {
                    TextButton(onClick = { showSourcePicker = false; galleryLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.gallery))
                    }
                    TextButton(onClick = { showSourcePicker = false; launchCamera() }) {
                        Text(stringResource(R.string.camera))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSourcePicker = false }) { Text(stringResource(R.string.Cancel)) } },
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.Cancel)) }
            Text(stringResource(R.string.New_message), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(64.dp))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            placeholder = { Text(stringResource(R.string.Enter_a_message)) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            minLines = 7,
        )

        if (isSending) LinearProgressIndicator(Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onTagsClick) { Text("#", style = MaterialTheme.typography.titleMedium) }
            IconButton(onClick = {
                if (attachmentUri != null) { attachmentUri = null; attachmentMime = null }
                else showSourcePicker = true
            }) {
                Text(if (attachmentUri != null) "📎✓" else "📎", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(
                onClick = {
                    if (sendEnabled && !isSending) {
                        isSending = true
                        scope.launch {
                            App.instance.sendMessage(scope, messagePosted, textFieldValue.text, attachmentUri, attachmentMime)
                        }
                    }
                },
                enabled = sendEnabled && !isSending,
            ) { Text(stringResource(R.string.Send)) }
        }
    }
}
