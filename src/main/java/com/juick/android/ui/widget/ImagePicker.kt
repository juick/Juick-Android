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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.juick.R
import com.juick.android.ui.JuickTheme
import kotlinx.coroutines.launch
import java.io.File

class ImagePickerState internal constructor() {
    internal var showSourcePicker by mutableStateOf(false)

    fun pick() {
        showSourcePicker = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberImagePicker(onImagePicked: (Uri) -> Unit): ImagePickerState {
    val context = LocalContext.current
    val state = remember { ImagePickerState() }
    val onPicked by rememberUpdatedState(onImagePicked)
    var cropSource by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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
        val colors = JuickTheme.colors
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        fun choose(action: () -> Unit) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                state.showSourcePicker = false
                action()
            }
        }
        ModalBottomSheet(
            onDismissRequest = { state.showSourcePicker = false },
            sheetState = sheetState,
            containerColor = colors.textBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SourceTile(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.gallery),
                    tint = colors.accent,
                    modifier = Modifier.weight(1f),
                ) {
                    choose { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
                SourceTile(
                    icon = Icons.Outlined.PhotoCamera,
                    label = stringResource(R.string.camera),
                    tint = colors.primary,
                    modifier = Modifier.weight(1f),
                ) {
                    choose {
                        val file = File(context.filesDir, "camera_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                }
            }
        }
    }
    return state
}

@Composable
private fun SourceTile(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = JuickTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.feed)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(30.dp), tint = tint)
        }
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.text)
    }
}
