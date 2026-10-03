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
package com.juick.android.ui.screens.feed

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.Utils
import com.juick.android.ui.widget.rememberImagePicker
import com.juick.api.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

@Composable
fun ProfileHeader(
    uname: String,
    currentUser: User?,
    onProfileChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signedIn = currentUser != null && currentUser.uid > 0
    val isOwnBlog = signedIn && currentUser?.uname == uname
    var menuExpanded by remember { mutableStateOf(false) }
    var avatarVersion by remember { mutableIntStateOf(0) }

    fun runAction(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                onProfileChanged()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sendCommand(command: String) = runAction {
        App.instance.api.newPost(command.toRequestBody("text/plain".toMediaTypeOrNull()), null)
    }

    val imagePicker = rememberImagePicker { croppedUri ->
        val mime = Utils.getMimeTypeFor(context, croppedUri) ?: "image/jpeg"
        if (!Utils.isImageTypeAllowed(mime)) {
            Toast.makeText(context, R.string.wrong_image_format, Toast.LENGTH_LONG).show()
            return@rememberImagePicker
        }
        runAction {
            App.instance.uploadAvatar(croppedUri, mime)
            avatarVersion++
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ownAvatar = currentUser?.avatar.orEmpty()
        val avatar = if (isOwnBlog && ownAvatar.isNotEmpty()) ownAvatar else "https://juick.com/a/$uname"
        AsyncImage(
            model = if (avatarVersion > 0) "$avatar?v=$avatarVersion" else avatar,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "@$uname",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.blog),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isOwnBlog) {
            IconButton(onClick = { imagePicker.pick() }) {
                Icon(Icons.Default.Edit, null)
            }
        } else if (signedIn && currentUser != null) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.Subscribe_to) + " @" + uname) },
                        onClick = { menuExpanded = false; sendCommand("S @$uname") },
                    )
                    if (currentUser.premium || currentUser.admin) {
                        val isVip = currentUser.vip.any { it.uname == uname }
                        DropdownMenuItem(
                            text = { Text(stringResource(if (isVip) R.string.remove_from_vip else R.string.add_to_vip)) },
                            onClick = {
                                menuExpanded = false
                                runAction {
                                    val response = App.instance.api.toggleVIP(uname)
                                    if (!response.isSuccessful) throw HttpException(response)
                                }
                            },
                        )
                    }
                    val isIgnored = currentUser.ignored.any { it.uname == uname }
                    DropdownMenuItem(
                        text = { Text(stringResource(if (isIgnored) R.string.remove_from_ignore_list else R.string.add_to_ignore_list) + " @" + uname) },
                        onClick = { menuExpanded = false; sendCommand("BL @$uname") },
                    )
                }
            }
        }
    }
}
