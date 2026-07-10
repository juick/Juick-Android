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

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageView
import com.juick.R
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropSheet(
    imageUri: Uri,
    onCropResult: (Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var cropImageView by remember { mutableStateOf<CropImageView?>(null) }
    var isCropping by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            AndroidView(
                factory = { ctx ->
                    CropImageView(ctx).apply {
                        setImageUriAsync(imageUri)
                        cropImageView = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.Cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextButton(
                    onClick = {
                        if (isCropping) return@TextButton
                        isCropping = true
                        cropImageView?.setOnCropImageCompleteListener { _, result ->
                            isCropping = false
                            val uri = if (result.isSuccessful) {
                                try {
                                    saveBitmapToFile(context, result.bitmap)
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                            onCropResult(uri)
                        }
                        cropImageView?.croppedImageAsync()
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.crop),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun saveBitmapToFile(context: android.content.Context, bitmap: Bitmap?): Uri? {
    if (bitmap == null) return null
    return try {
        val dir = File(context.filesDir, "cropped")
        dir.mkdirs()
        val file = File(dir, "crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file,
        )
    } catch (e: Exception) {
        null
    }
}
