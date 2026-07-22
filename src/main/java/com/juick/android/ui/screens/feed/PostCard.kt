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

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.util.MessageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val quoteColor = Color(0xFF666666)

private fun List<UrlPosition>.urlAt(offset: Int): String? =
    firstOrNull { it.start <= offset && offset < it.end }?.url

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
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxWidth().clickable { onPostClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        color = colors.surface, shadowElevation = 1.dp, tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(post.user.avatar, null, Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(10.dp))
                Text(post.user.uname, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.primary, modifier = Modifier.clickable { onUserClick() })
                Spacer(Modifier.weight(1f))
                Text(MessageUtils.formatMessageTimestamp(post), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                    }
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
                                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                                    menuExpanded = false
                                    scope.launch { App.instance.api.togglePrivacy(post.mid) }
                                })
                            }
                            val deleteLabel = if (post.rid == 0) R.string.DeletePost else R.string.DeleteComment
                            DropdownMenuItem(text = { Text(stringResource(deleteLabel)) }, onClick = {
                                menuExpanded = false
                                val cmd = if (post.rid == 0) "D #${post.mid}" else "D #${post.mid}/${post.rid}"
                                val receiver = MutableStateFlow<Result<PostResponse>?>(null)
                                App.instance.sendMessage(scope, receiver, cmd)
                                onDeletePost()
                            })
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            val blocks = remember(post) {
                formatPostBlocks(post, colors.primary, colors.onSurfaceVariant, colors.onSurface, quoteColor)
            }
            Column {
                for (block in blocks) {
                    when (block) {
                        is TextBlock.Regular -> {
                            ClickableText(
                                text = block.annotatedString,
                                style = MaterialTheme.typography.bodyMedium,
                                onClick = { offset -> block.urlPositions.urlAt(offset)?.let(onLinkClick) },
                            )
                        }
                        is TextBlock.Quote -> {
                            Surface(
                                color = colors.background,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp)
                                    .drawWithContent { drawContent(); drawRect(colors.tertiary, Offset(0f, 0f), Size(3.dp.toPx(), size.height)) },
                            ) {
                                ClickableText(
                                    text = block.annotatedString,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = quoteColor),
                                    modifier = Modifier.padding(8.dp),
                                    onClick = { offset -> block.urlPositions.urlAt(offset)?.let(onLinkClick) },
                                )
                            }
                        }
                    }
                }
            }

            val photo = post.photo
            val medium = photo?.medium
            val imageUrl = medium?.url
            if (!imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(imageUrl, null, Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.FillWidth)
            }

            Spacer(Modifier.height(12.dp))
            if (showCounters) {
                HorizontalDivider(color = colors.outlineVariant, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    val likeColor = if (post.liked) colors.tertiary else colors.onSurfaceVariant
                    Icon(painterResource(R.drawable.ic_ei_heart), null, Modifier.size(18.dp).clickable { onLikeClick() }, tint = likeColor)
                    Text("${post.likes}", style = MaterialTheme.typography.labelSmall, color = likeColor)
                    Icon(painterResource(R.drawable.ic_ei_comment), null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                    Text("${post.replies}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
