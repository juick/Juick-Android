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
package com.juick.android.ui.screens.chats

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.AvatarShape
import com.juick.android.ui.CardShape
import com.juick.android.ui.JuickTheme
import com.juick.api.model.Chat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onChatClick: (Chat) -> Unit,
    onNavigateToAuth: () -> Unit,
) {
    var chatsState by remember { mutableStateOf<Result<List<Chat>>?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit, refreshTrigger) {
        if (App.instance.isAuthenticated) {
            try {
                chatsState = Result.success(withContext(Dispatchers.IO) { App.instance.api.groupsPms(10).pms })
            } catch (e: CancellationException) { throw e } catch (e: Exception) { chatsState = Result.failure(e) }
            isRefreshing = false
        } else {
            onNavigateToAuth()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; refreshTrigger++ },
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val result = chatsState) {
            null -> ChatsPanel { repeat(6) { index -> SkeletonRow(divider = index > 0) } }
            else -> result.fold(
                onSuccess = { chats ->
                    if (chats.isEmpty()) {
                        EmptyChats(stringResource(R.string.you_have_no_direct_messages))
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            itemsIndexed(chats, key = { _, chat -> chat.uid }) { index, chat ->
                                val shape = RoundedCornerShape(
                                    topStart = if (index == 0) 16.dp else 0.dp,
                                    topEnd = if (index == 0) 16.dp else 0.dp,
                                    bottomStart = if (index == chats.lastIndex) 16.dp else 0.dp,
                                    bottomEnd = if (index == chats.lastIndex) 16.dp else 0.dp,
                                )
                                ChatListItem(
                                    chat = chat,
                                    divider = index > 0,
                                    onClick = { onChatClick(chat) },
                                    modifier = Modifier.animateItem().clip(shape).background(JuickTheme.colors.textBackground),
                                )
                            }
                        }
                    }
                },
                onFailure = { EmptyChats(stringResource(R.string.network_error)) },
            )
        }
    }
}

@Composable
private fun ChatsPanel(content: @Composable ColumnScope.() -> Unit) {
    val colors = JuickTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(CardShape)
            .background(colors.textBackground)
            .border(1.dp, colors.border, CardShape),
        content = content,
    )
}

@Composable
private fun EmptyChats(message: String) {
    val colors = JuickTheme.colors
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_ei_envelope), null, Modifier.size(44.dp), tint = colors.accent)
        }
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun SkeletonRow(divider: Boolean) {
    val colors = JuickTheme.colors
    val bone = colors.border
    Column {
        if (divider) Box(Modifier.padding(start = 84.dp).fillMaxWidth().height(1.dp).background(colors.border))
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(AvatarShape).background(bone))
            Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(110.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(bone))
                Box(Modifier.size(180.dp, 12.dp).clip(RoundedCornerShape(4.dp)).background(bone))
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    divider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JuickTheme.colors
    val context = LocalContext.current
    val unread = chat.unreadCount
    val last = chat.getLastMessage()
    Column(modifier.fillMaxWidth().clickable { onClick() }) {
        if (divider) Box(Modifier.padding(start = 84.dp).fillMaxWidth().height(1.dp).background(colors.border))
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = chat.dialogPhoto,
                contentDescription = null,
                placeholder = painterResource(R.drawable.av_96),
                error = painterResource(R.drawable.av_96),
                modifier = Modifier.size(56.dp).clip(AvatarShape).background(colors.feed),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        chat.dialogName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    last.getTimestamp()?.let {
                        Text(
                            shortDate(context, it),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (unread > 0) colors.accent else colors.dimmed,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        last.getBody().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unread > 0) colors.text else colors.dimmed,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread > 0) {
                        Box(
                            Modifier
                                .padding(start = 8.dp, top = 2.dp)
                                .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$unread", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun shortDate(context: Context, date: Date): String {
    val time = date.time
    val now = System.currentTimeMillis()
    return when {
        DateUtils.isToday(time) -> DateFormat.getTimeFormat(context).format(date)
        DateUtils.isToday(time + DateUtils.DAY_IN_MILLIS) ->
            DateUtils.getRelativeTimeSpanString(time, now, DateUtils.DAY_IN_MILLIS).toString()
        now - time < 7 * DateUtils.DAY_IN_MILLIS -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
    }
}
