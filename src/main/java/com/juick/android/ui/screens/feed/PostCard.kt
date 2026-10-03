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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.ui.JuickDivider
import com.juick.android.ui.JuickTheme
import com.juick.api.model.LinkPreview
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.util.MessageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private fun List<UrlPosition>.urlAt(offset: Int): String? =
    firstOrNull { it.start <= offset && offset < it.end }?.url

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    onPostClick: () -> Unit,
    onUserClick: () -> Unit,
    onMenuClick: () -> Unit,
    onLikeClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    showCounters: Boolean = true,
    currentUid: Int = 0,
    isPremiumOrAdmin: Boolean = false,
    isAuthenticated: Boolean = false,
    onDeletePost: () -> Unit = {},
    onSubscribeToggle: (() -> Unit)? = null,
    onReplyToClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = JuickTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val currentOnDeletePost by rememberUpdatedState(onDeletePost)
    val isReply = post.rid > 0

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(R.string.Are_you_sure_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val cmd = if (post.rid == 0) "D #${post.mid}" else "D #${post.mid}/${post.rid}"
                    val result = MutableStateFlow<Result<PostResponse>?>(null)
                    App.instance.sendMessage(scope, result, cmd)
                    scope.launch {
                        result.filterNotNull().first().fold(
                            onSuccess = { currentOnDeletePost() },
                            onFailure = { Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show() },
                        )
                    }
                }) { Text(stringResource(R.string.Yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.Cancel)) }
            },
        )
    }

    val menuDots = @Composable {
        Box {
            Icon(
                Icons.Default.MoreVert,
                stringResource(R.string.context_menu),
                Modifier.clickable { menuExpanded = true; onMenuClick() },
                tint = colors.darkerGray,
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.Share)) }, onClick = {
                    menuExpanded = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "https://juick.com/m/${post.mid}")
                    }
                    context.startActivity(intent)
                })
                if (currentUid > 0 && post.user.uid == currentUid) {
                    if (isPremiumOrAdmin && post.rid == 0) {
                        val label = if (post.friendsOnly) R.string.make_public else R.string.make_private
                        val icon = if (post.friendsOnly) R.drawable.ic_ei_unlock else R.drawable.ic_ei_lock
                        DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            leadingIcon = { Icon(painterResource(icon), null) },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    try {
                                        App.instance.api.togglePrivacy(post.mid)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                        )
                    }
                    val deleteLabel = if (post.rid == 0) R.string.DeletePost else R.string.DeleteComment
                    DropdownMenuItem(text = { Text(stringResource(deleteLabel)) }, onClick = {
                        menuExpanded = false
                        confirmDelete = true
                    })
                }
            }
        }
    }

    val avatar = @Composable {
        AsyncImage(
            post.user.avatar, stringResource(R.string.user_photo),
            Modifier.size(48.dp).clickable { onUserClick() },
            placeholder = painterResource(R.drawable.av_96),
            error = painterResource(R.drawable.av_96),
            contentScale = ContentScale.Crop,
        )
    }

    val username = @Composable {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                post.user.uname,
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
                modifier = Modifier.clickable { onUserClick() },
            )
            if (post.user.premium) {
                Icon(painterResource(R.drawable.ic_ei_star), stringResource(R.string.premium_badge), tint = colors.premium)
            }
        }
    }

    val body = @Composable { textModifier: Modifier ->
        val blocks = remember(post, colors) {
            formatPostBlocks(post, colors.dimmed, colors.dimmed, colors.text, colors.text)
        }
        if (blocks.isNotEmpty()) {
            Column(textModifier) {
                for (block in blocks) {
                    val onClick: (Int) -> Unit = { offset: Int ->
                        block.urlPositions.urlAt(offset)?.let(onLinkClick) ?: onPostClick()
                    }
                    when (block) {
                        is TextBlock.Regular -> ClickableText(
                            text = block.annotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                            onClick = onClick,
                        )
                        is TextBlock.Quote -> ClickableText(
                            text = block.annotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                            modifier = Modifier
                                .drawBehind { drawRect(colors.dimmed, Offset.Zero, Size(2.dp.toPx(), size.height)) }
                                .padding(start = 4.dp),
                            onClick = onClick,
                        )
                    }
                }
            }
        }
    }

    val media = @Composable {
        val photo = post.photo
        val imageUrl = photo?.medium?.url
        if (!imageUrl.isNullOrBlank()) {
            val hideNsfw = BuildConfig.HIDE_NSFW && MessageUtils.haveNSFWContent(post)
            val request = remember(imageUrl, hideNsfw) {
                ImageRequest.Builder(context).data(imageUrl)
                    .apply { if (hideNsfw) transformations(PixelateTransformation()) }
                    .build()
            }
            AsyncImage(
                request, stringResource(R.string.attached_photo),
                Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onLinkClick(photo.url ?: imageUrl) },
                contentScale = ContentScale.FillWidth,
            )
        } else {
            val preview by produceState<LinkPreview?>(null, post.mid, post.rid) {
                val text = post.getText()
                val previewer = App.instance.previewers.firstOrNull { it.hasViewableContent(text) }
                    ?: return@produceState
                value = suspendCancellableCoroutine { cont -> previewer.getPreviewUrl(text) { cont.resume(it) } }
            }
            preview?.let { link ->
                Box(Modifier.fillMaxWidth().clickable { onLinkClick(link.source) }) {
                    AsyncImage(
                        link.url, stringResource(R.string.attached_photo),
                        Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        contentScale = ContentScale.FillWidth,
                    )
                    if (link.description.isNotEmpty()) {
                        Text(
                            link.description,
                            color = Color.White,
                            modifier = Modifier
                                .padding(top = if (isReply) 20.dp else 16.dp)
                                .background(Color(0xAA000000))
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onPostClick, onLongClick = { menuExpanded = true })

    if (isReply) {
        Column(cardModifier.background(colors.textBackground)) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) { avatar() }
                Column(Modifier.weight(1f).padding(top = 16.dp)) {
                    username()
                    val to = post.to
                    if (post.replyto > 0 && to != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = onReplyToClick != null) { onReplyToClick?.invoke(post.replyto) }
                                .padding(6.dp),
                        ) {
                            AsyncImage(to.avatar, null, Modifier.size(24.dp), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(2.dp))
                            Text(to.uname, style = MaterialTheme.typography.titleSmall, color = colors.primary)
                        }
                    }
                    body(Modifier.padding(end = 16.dp))
                }
                Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) { menuDots() }
            }
            Box(Modifier.padding(top = 8.dp)) { media() }
            Text(
                MessageUtils.formatMessageTimestamp(post),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.darkerGray,
                modifier = Modifier.padding(start = 80.dp, top = 16.dp),
            )
            Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(Color(0xFFE1E1E1)))
        }
        return
    }

    Column(cardModifier.padding(vertical = 8.dp)) {
        JuickDivider()
        Column(Modifier.fillMaxWidth().background(colors.textBackground).padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                avatar()
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    username()
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            MessageUtils.formatMessageTimestamp(post),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.darkerGray,
                        )
                        if (post.friendsOnly) {
                            Icon(painterResource(R.drawable.ic_ei_lock), null, tint = colors.darkerGray)
                        }
                    }
                }
                menuDots()
            }
            body(Modifier.padding(top = 16.dp))
        }
        Box(Modifier.fillMaxWidth().background(colors.textBackground)) { media() }
        if (showCounters || onSubscribeToggle != null) {
            JuickDivider()
            Row(
                Modifier.fillMaxWidth().background(colors.textBackground).padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                val likeColor = if (post.liked) colors.accent else colors.darkerGray
                CounterLabel(
                    icon = R.drawable.ic_ei_heart,
                    text = if (post.likes > 0) "${post.likes}" else stringResource(R.string.recommend),
                    color = likeColor,
                    onClick = onLikeClick,
                )
                if (onSubscribeToggle != null) {
                    CounterLabel(
                        icon = if (post.subscribed) R.drawable.ic_ei_check else R.drawable.ic_ei_eye,
                        text = stringResource(if (post.subscribed) R.string.subscribed else R.string.subscribe),
                        color = colors.darkerGray,
                        onClick = onSubscribeToggle,
                    )
                } else {
                    CounterLabel(
                        icon = R.drawable.ic_ei_comment,
                        text = if (post.replies > 0) "${post.replies}" else stringResource(R.string.reply),
                        color = colors.darkerGray,
                        onClick = onPostClick,
                    )
                }
            }
        }
        JuickDivider()
    }
}

@Composable
private fun CounterLabel(icon: Int, text: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    ) {
        Icon(painterResource(icon), null, tint = color)
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = color)
    }
}
