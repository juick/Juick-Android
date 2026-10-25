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
package com.juick.android.ui.screens.chat

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.getSpans
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.ui.AvatarShape
import com.juick.android.ui.JuickTheme
import com.juick.android.ui.screens.feed.UrlPosition
import com.juick.api.model.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

private const val GROUP_GAP_MS = 5 * 60 * 1000L
private val BubbleRadius = 20.dp
private val BubbleJoin = 6.dp
private val OwnBubbleEnd = Color(0xFF8E5CFF)

@Composable
fun ChatScreen(
    uname: String,
    onUserClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    var messagesState by remember { mutableStateOf<Result<List<Post>>?>(null) }
    val messages = (messagesState?.getOrNull() ?: emptyList()).distinctBy(::messageKey)
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = JuickTheme.colors

    LaunchedEffect(uname) {
        try {
            messagesState = Result.success(withContext(Dispatchers.IO) { App.instance.api.pm(uname) })
        } catch (e: CancellationException) { throw e } catch (e: Exception) { messagesState = Result.failure(e) }
    }

    fun prepend(incoming: List<Post>) {
        val current = messagesState?.getOrNull() ?: return
        val fresh = incoming.filter { msg ->
            current.none { it.getTimestamp() == msg.getTimestamp() && it.getBody() == msg.getBody() }
        }
        if (fresh.isNotEmpty()) {
            messagesState = Result.success(fresh.reversed() + current)
            scope.launch { listState.animateScrollToItem(0) }
        }
    }

    LaunchedEffect(uname) {
        App.instance.messages.collect { newMessages ->
            prepend(newMessages.filter { it.mid == 0 && (it.user.uname == uname || it.to?.uname == uname) })
        }
    }

    fun send() {
        val text = inputText.trim()
        if (text.isEmpty() || isSending) return
        isSending = true
        scope.launch {
            try {
                prepend(listOf(App.instance.api.postPm(uname, text)))
                inputText = ""
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
            } finally {
                isSending = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.mainBackground)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                messagesState == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.accent)
                messagesState?.isFailure == true -> Text(
                    stringResource(R.string.network_error),
                    color = colors.dimmed,
                    modifier = Modifier.align(Alignment.Center),
                )
                messages.isEmpty() -> EmptyChat(uname, Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    itemsIndexed(messages, key = { _, post -> messageKey(post) }) { index, post ->
                        val older = messages.getOrNull(index + 1)
                        val newer = messages.getOrNull(index - 1)
                        val startsGroup = older == null || !sameGroup(older, post)
                        val endsGroup = newer == null || !sameGroup(post, newer)
                        Column(Modifier.animateItem()) {
                            val date = post.getTimestamp()
                            if (date != null && (older?.getTimestamp()?.let { !sameDay(it, date) } != false)) {
                                DayChip(dayLabel(context, date))
                            }
                            ChatBubble(
                                post = post,
                                isOwn = post.user.uname != uname,
                                startsGroup = startsGroup,
                                endsGroup = endsGroup,
                                onLinkClick = onLinkClick,
                                onUserClick = onUserClick,
                                modifier = Modifier.padding(top = if (startsGroup) 10.dp else 2.dp),
                            )
                        }
                    }
                }
            }
        }

        InputBar(
            text = inputText,
            onTextChange = { inputText = it },
            isSending = isSending,
            onSend = ::send,
        )
    }
}

@Composable
private fun InputBar(text: String, onTextChange: (String) -> Unit, isSending: Boolean, onSend: () -> Unit) {
    val colors = JuickTheme.colors
    val canSend = text.isNotBlank() && !isSending
    val buttonFallback by animateColorAsState(if (canSend) colors.accent else colors.border, label = "send")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .shadow(3.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(RoundedCornerShape(24.dp))
                .background(colors.textBackground)
                .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (text.isEmpty()) {
                Text(stringResource(R.string.Enter_a_message), style = MaterialTheme.typography.bodyLarge, color = colors.dimmed)
            }
        }
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (canSend) Brush.linearGradient(listOf(colors.accent, OwnBubbleEnd))
                    else SolidColor(buttonFallback)
                )
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            if (isSending) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.ArrowUpward,
                    stringResource(R.string.Send),
                    Modifier.size(22.dp),
                    tint = if (canSend) Color.White else colors.dimmed,
                )
            }
        }
    }
}

@Composable
private fun EmptyChat(uname: String, modifier: Modifier = Modifier) {
    val colors = JuickTheme.colors
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.accent.copy(alpha = 0.18f), OwnBubbleEnd.copy(alpha = 0.18f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("👋", fontSize = 40.sp)
        }
        Text(
            "@$uname",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun DayChip(label: String) {
    val colors = JuickTheme.colors
    Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.dimmed,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.border.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun ChatBubble(
    post: Post,
    isOwn: Boolean,
    onLinkClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    startsGroup: Boolean = true,
    endsGroup: Boolean = true,
) {
    val colors = JuickTheme.colors
    val context = LocalContext.current
    val body = post.getBody().orEmpty()
    val emojiOnly = remember(body) { isEmojiOnly(body) }
    val textColor = if (isOwn) Color.White else colors.text

    val (annotatedText, urlPositions) = remember(post, isOwn, colors) {
        val spannable = android.text.SpannableString(body)
        android.text.util.Linkify.addLinks(spannable, android.text.util.Linkify.ALL)
        val urls = spannable.getSpans<android.text.style.URLSpan>().map {
            UrlPosition(spannable.getSpanStart(it), spannable.getSpanEnd(it), it.url)
        }
        buildAnnotatedString {
            append(body)
            for (url in urls) addStyle(SpanStyle(color = if (isOwn) Color.White else colors.primary, textDecoration = TextDecoration.Underline), url.start, url.end)
        } to urls
    }

    val join = { inner: Boolean -> if (inner) BubbleJoin else BubbleRadius }
    val shape = if (isOwn) {
        RoundedCornerShape(BubbleRadius, join(!startsGroup), join(!endsGroup), BubbleRadius)
    } else {
        RoundedCornerShape(join(!startsGroup), BubbleRadius, BubbleRadius, join(!endsGroup))
    }
    val time = post.getTimestamp()?.let { DateFormat.getTimeFormat(context).format(it) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isOwn) {
            Spacer(Modifier.width(56.dp))
        } else {
            Box(Modifier.size(32.dp)) {
                if (endsGroup) {
                    AsyncImage(
                        post.user.avatar, null,
                        Modifier.fillMaxSize().clip(AvatarShape).background(colors.feed).clickable { onUserClick(post.user.uname) },
                        placeholder = painterResource(R.drawable.av_96),
                        error = painterResource(R.drawable.av_96),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        if (emojiOnly) {
            Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                Text(body, fontSize = 44.sp, lineHeight = 52.sp)
                if (endsGroup && time != null) {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = colors.dimmed)
                }
            }
        } else {
            val background = if (isOwn) {
                Modifier.background(Brush.linearGradient(listOf(colors.accent, OwnBubbleEnd)), shape)
            } else {
                Modifier.background(colors.border, shape)
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .clip(shape)
                    .then(background)
                    .padding(start = 14.dp, end = 14.dp, top = 9.dp, bottom = if (endsGroup) 6.dp else 9.dp),
                horizontalAlignment = Alignment.End,
            ) {
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor, lineHeight = 22.sp),
                    onClick = { offset ->
                        urlPositions.firstOrNull { it.start <= offset && offset < it.end }?.let { onLinkClick(it.url) }
                    },
                )
                if (endsGroup && time != null) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOwn) Color.White.copy(alpha = 0.75f) else colors.dimmed,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        if (!isOwn) Spacer(Modifier.width(56.dp))
    }
}

private fun messageKey(post: Post) = "${post.getTimestamp()?.time}_${post.user.uname}_${post.getBody()}"

private fun sameGroup(older: Post, newer: Post): Boolean {
    if (older.user.uname != newer.user.uname) return false
    val a = older.getTimestamp() ?: return true
    val b = newer.getTimestamp() ?: return true
    return b.time - a.time < GROUP_GAP_MS
}

private fun sameDay(a: Date, b: Date): Boolean {
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun dayLabel(context: Context, date: Date): String {
    val time = date.time
    return if (DateUtils.isToday(time) || DateUtils.isToday(time + DateUtils.DAY_IN_MILLIS)) {
        DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
    } else {
        DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL)
    }
}

private fun isEmojiOnly(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > 8) return false
    var hasSymbol = false
    var i = 0
    while (i < trimmed.length) {
        val cp = trimmed.codePointAt(i)
        when (Character.getType(cp)) {
            Character.OTHER_SYMBOL.toInt() -> hasSymbol = true
            Character.NON_SPACING_MARK.toInt(), Character.FORMAT.toInt(), Character.SPACE_SEPARATOR.toInt(),
            Character.MODIFIER_SYMBOL.toInt(), Character.ENCLOSING_MARK.toInt() -> {}
            else -> return false
        }
        i += Character.charCount(cp)
    }
    return hasSymbol
}
