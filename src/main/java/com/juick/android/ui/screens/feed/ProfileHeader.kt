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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.Utils
import com.juick.android.ui.JuickTheme
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
    var blogAvatar by remember(uname) { mutableStateOf<String?>(null) }

    LaunchedEffect(uname) {
        blogAvatar = try {
            App.instance.api.info(uname).avatar
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

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

    val colors = JuickTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.textBackground)
            .padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val ownAvatar = currentUser?.avatar.orEmpty()
            val avatar = if (isOwnBlog && ownAvatar.isNotEmpty()) ownAvatar else blogAvatar
            AsyncImage(
                model = if (avatar != null && avatarVersion > 0) "$avatar?v=$avatarVersion" else avatar,
                contentDescription = stringResource(R.string.Juick_profile),
                placeholder = painterResource(R.drawable.av_96),
                error = painterResource(R.drawable.av_96),
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Crop,
            )
            if (isOwnBlog) {
                TextButton(
                    onClick = { imagePicker.pick() },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_ei_pencil), null, Modifier.size(18.dp), tint = colors.accent)
                    Spacer(Modifier.width(2.dp))
                    Text(stringResource(R.string.avatar_change).uppercase(), fontSize = 12.sp, color = colors.accent)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = uname,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
            )
            Text(
                text = stringResource(R.string.blog),
                fontSize = 14.sp,
                color = colors.dimmed,
            )
        }
        if (!isOwnBlog && signedIn && currentUser != null) {
            Box(Modifier.padding(start = 16.dp)) {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.context_menu), tint = colors.text)
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
