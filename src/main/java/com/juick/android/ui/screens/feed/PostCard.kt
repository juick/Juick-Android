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

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.ui.AvatarShape
import com.juick.android.ui.CardShape
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

private val MediaShape = RoundedCornerShape(14.dp)
private val PanelRadius = 12.dp
private const val COLLAPSE_LIMIT = 512

/** Cuts the blocks at [limit] characters, at the last space before it, and adds an ellipsis. Null when they already fit. */
private fun List<TextBlock>.collapsed(limit: Int): List<TextBlock>? {
    if (sumOf { it.annotatedString.length } <= limit) return null
    var budget = limit
    val out = mutableListOf<TextBlock>()
    for (block in this) {
        val text = block.annotatedString
        if (text.length <= budget) {
            out += block
            budget -= text.length
            continue
        }
        val cut = text.text.lastIndexOf(' ', budget - 1).takeIf { it > 0 } ?: budget
        val part = text.subSequence(0, cut) + AnnotatedString("…")
        out += when (block) {
            is TextBlock.Regular -> TextBlock.Regular(part, block.urlPositions)
            is TextBlock.Quote -> TextBlock.Quote(part, block.urlPositions)
        }
        break
    }
    return out
}

private fun relativeTime(post: Post): String {
    val time = post.getTimestamp()?.time ?: return ""
    return DateUtils.getRelativeTimeSpanString(
        time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

/** One segment of a vertically stacked panel: only the outer corners are rounded, no edges between segments. */
private fun Modifier.panelSegment(top: Boolean, bottom: Boolean, fill: Color, stroke: Color) = drawBehind {
    val r = PanelRadius.toPx()
    val tr = if (top) r else 0f
    val br = if (bottom) r else 0f
    val w = size.width
    val h = size.height
    val fillPath = Path().apply {
        addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(tr), CornerRadius(tr), CornerRadius(br), CornerRadius(br)))
    }
    drawPath(fillPath, fill)
    val sw = 1.dp.toPx()
    val i = sw / 2
    val outline = Path().apply {
        moveTo(i, h - br)
        lineTo(i, if (top) tr + i else 0f)
        if (top) {
            arcTo(Rect(i, i, 2 * r - i, 2 * r - i), 180f, 90f, false)
            lineTo(w - r, i)
            arcTo(Rect(w - 2 * r + i, i, w - i, 2 * r - i), 270f, 90f, false)
        } else {
            moveTo(w - i, 0f)
        }
        lineTo(w - i, h - br)
        if (bottom) {
            arcTo(Rect(w - 2 * r + i, h - 2 * r + i, w - i, h - i), 0f, 90f, false)
            lineTo(r, h - i)
            arcTo(Rect(i, h - 2 * r + i, 2 * r - i, h - i), 90f, 90f, false)
        }
    }
    drawPath(outline, stroke, style = Stroke(sw))
}

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
    onReplyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    inThread: Boolean = false,
    panelTop: Boolean = true,
    panelBottom: Boolean = true,
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
            containerColor = colors.textBackground,
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
                }) { Text(stringResource(R.string.Yes), color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.Cancel), color = colors.dimmed) }
            },
        )
    }

    val menuDots = @Composable {
        Box {
            Icon(
                Icons.Default.MoreVert,
                stringResource(R.string.context_menu),
                Modifier
                    .clip(CircleShape)
                    .clickable { menuExpanded = true; onMenuClick() }
                    .padding(2.dp)
                    .size(20.dp),
                tint = colors.dimmed,
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = colors.textBackground,
            ) {
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
                    DropdownMenuItem(text = { Text(stringResource(deleteLabel), color = colors.accent) }, onClick = {
                        menuExpanded = false
                        confirmDelete = true
                    })
                }
            }
        }
    }

    val avatar = @Composable { size: Dp ->
        AsyncImage(
            post.user.avatar, stringResource(R.string.user_photo),
            Modifier
                .size(size)
                .clip(AvatarShape)
                .background(colors.feed)
                .clickable { onUserClick() },
            placeholder = painterResource(R.drawable.av_96),
            error = painterResource(R.drawable.av_96),
            contentScale = ContentScale.Crop,
        )
    }

    val nameLine = @Composable { large: Boolean ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                post.user.uname,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onUserClick() },
            )
            if (post.user.premium) {
                Icon(
                    painterResource(R.drawable.ic_ei_star), stringResource(R.string.premium_badge),
                    Modifier.padding(start = 2.dp).size(18.dp), tint = colors.premium,
                )
            }
            if (post.friendsOnly) {
                Icon(painterResource(R.drawable.ic_ei_lock), null, Modifier.size(18.dp), tint = colors.dimmed)
            }
        }
    }

    val timeText = @Composable {
        Text(
            relativeTime(post),
            style = MaterialTheme.typography.bodySmall,
            color = colors.dimmed,
            maxLines = 1,
        )
    }

    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text, lineHeight = 23.sp)

    val body = @Composable {
        val full = remember(post, colors) {
            formatPostBlocks(post, colors.primary, colors.dimmed, colors.text, colors.quote)
        }
        val collapse = !inThread && !isReply
        val cut = remember(full, collapse) { if (collapse) full.collapsed(COLLAPSE_LIMIT) else null }
        val blocks = cut ?: full
        val collapsed = cut != null
        if (blocks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (block in blocks) {
                    val onClick: (Int) -> Unit = { offset: Int ->
                        block.urlPositions.urlAt(offset)?.let(onLinkClick) ?: onPostClick()
                    }
                    when (block) {
                        is TextBlock.Regular -> ClickableText(
                            text = block.annotatedString,
                            style = bodyStyle,
                            onClick = onClick,
                        )
                        is TextBlock.Quote -> ClickableText(
                            text = block.annotatedString,
                            style = bodyStyle,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .fillMaxWidth()
                                .background(colors.feed)
                                .drawBehind { drawRect(colors.accent, Offset.Zero, Size(3.dp.toPx(), size.height)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            onClick = onClick,
                        )
                    }
                }
                if (collapsed) {
                    Text(
                        stringResource(R.string.read_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                    )
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
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(MediaShape)
                    .background(colors.feed)
                    .clickable { onLinkClick(photo.url ?: imageUrl) },
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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(MediaShape)
                        .background(colors.feed)
                        .clickable { onLinkClick(link.source) },
                ) {
                    AsyncImage(
                        link.url, stringResource(R.string.attached_photo),
                        Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )
                    if (link.description.isNotEmpty()) {
                        Text(
                            link.description,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color(0x99000000))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (isReply) {
        val replyModifier = modifier
            .fillMaxWidth()
            .panelSegment(panelTop, panelBottom, colors.textBackground, colors.border)
            .clip(
                RoundedCornerShape(
                    topStart = if (panelTop) PanelRadius else 0.dp, topEnd = if (panelTop) PanelRadius else 0.dp,
                    bottomStart = if (panelBottom) PanelRadius else 0.dp, bottomEnd = if (panelBottom) PanelRadius else 0.dp,
                )
            )
            .combinedClickable(onClick = onPostClick, onLongClick = { menuExpanded = true })
            .padding(horizontal = 16.dp, vertical = 12.dp)
        Row(replyModifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            avatar(34.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f, fill = false)) { nameLine(false) }
                        timeText()
                    }
                    menuDots()
                }
                val to = post.to
                if (post.replyto > 0 && to != null && to.uid != post.user.uid) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.feed)
                            .clickable(enabled = onReplyToClick != null) { onReplyToClick?.invoke(post.replyto) }
                            .padding(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(12.dp), tint = colors.dimmed)
                        AsyncImage(
                            to.avatar, null,
                            Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Text(
                            to.uname,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            maxLines = 1,
                        )
                    }
                }
                body()
                media()
                onReplyClick?.let {
                    ActionLabel(
                        icon = R.drawable.ic_ei_comment,
                        text = stringResource(R.string.reply),
                        color = colors.tag,
                        onClick = it,
                    )
                }
            }
        }
        return
    }

    Column(
        modifier
            .fillMaxWidth()
            .shadow(2.dp, CardShape, ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.15f))
            .clip(CardShape)
            .background(colors.textBackground)
            .border(1.dp, colors.border, CardShape)
            .combinedClickable(onClick = onPostClick, onLongClick = { menuExpanded = true })
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            avatar(if (inThread) 48.dp else 42.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                nameLine(inThread)
                timeText()
            }
            menuDots()
        }
        body()
        media()
        if (showCounters || onSubscribeToggle != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                ActionLabel(
                    icon = R.drawable.ic_ei_heart,
                    text = if (post.likes > 0) "${post.likes}" else stringResource(R.string.recommend),
                    color = if (post.liked) colors.accent else colors.tag,
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f),
                )
                if (onSubscribeToggle != null) {
                    ActionLabel(
                        icon = if (post.subscribed) R.drawable.ic_ei_check else R.drawable.ic_ei_eye,
                        text = stringResource(if (post.subscribed) R.string.subscribed else R.string.subscribe),
                        color = if (post.subscribed) colors.primary else colors.tag,
                        onClick = onSubscribeToggle,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    ActionLabel(
                        icon = R.drawable.ic_ei_comment,
                        text = if (post.replies > 0) "${post.replies}" else stringResource(R.string.reply),
                        color = colors.tag,
                        onClick = onReplyClick ?: onPostClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionLabel(icon: Int, text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(painterResource(icon), null, Modifier.size(25.dp), tint = color)
            Spacer(Modifier.width(2.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
