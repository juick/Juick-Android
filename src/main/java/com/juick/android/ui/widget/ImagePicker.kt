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
package com.juick.android.ui.widget

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.juick.R
import java.io.File

class ImagePickerState internal constructor() {
    internal var showSourcePicker by mutableStateOf(false)

    fun pick() {
        showSourcePicker = true
    }
}

@Composable
fun rememberImagePicker(onImagePicked: (Uri) -> Unit): ImagePickerState {
    val context = LocalContext.current
    val state = remember { ImagePickerState() }
    val onPicked by rememberUpdatedState(onImagePicked)
    var cropSource by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { cropSource = it }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { cropSource = it }
    }

    cropSource?.let { sourceUri ->
        CropSheet(
            imageUri = sourceUri,
            onCropResult = { croppedUri ->
                cropSource = null
                croppedUri?.let(onPicked)
            },
            onDismiss = { cropSource = null },
        )
    }

    if (state.showSourcePicker) {
        AlertDialog(
            onDismissRequest = { state.showSourcePicker = false },
            text = {
                Column {
                    TextButton(onClick = { state.showSourcePicker = false; galleryLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.gallery))
                    }
                    TextButton(onClick = {
                        state.showSourcePicker = false
                        val file = File(context.filesDir, "camera_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }) {
                        Text(stringResource(R.string.camera))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { state.showSourcePicker = false }) { Text(stringResource(R.string.Cancel)) } },
        )
    }
    return state
}
