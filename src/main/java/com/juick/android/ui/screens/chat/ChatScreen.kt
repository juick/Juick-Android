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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.text.getSpans
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.juick.App
import com.juick.R
import com.juick.android.ui.JuickTheme
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.juick.android.ui.screens.feed.UrlPosition
import com.juick.api.model.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uname: String,
    onUserClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    var messagesState by remember { mutableStateOf<Result<List<Post>>?>(null) }
    val messages = (messagesState?.getOrNull() ?: emptyList()).reversed()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(uname) {
        try {
            messagesState = Result.success(withContext(Dispatchers.IO) { App.instance.api.pm(uname) })
        } catch (e: CancellationException) { throw e } catch (e: Exception) { messagesState = Result.failure(e) }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun prepend(incoming: List<Post>) {
        val current = messagesState?.getOrNull() ?: return
        val fresh = incoming.filter { msg ->
            current.none { it.getTimestamp() == msg.getTimestamp() && it.getBody() == msg.getBody() }
        }
        if (fresh.isNotEmpty()) messagesState = Result.success(fresh.reversed() + current)
    }

    LaunchedEffect(uname) {
        App.instance.messages.collect { newMessages ->
            prepend(newMessages.filter { it.mid == 0 && (it.user.uname == uname || it.to?.uname == uname) })
        }
    }

    val colors = JuickTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.mainBackground)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            itemsIndexed(messages) { index, post ->
                val day = post.getTimestamp()?.let { dayFormat.format(it) }
                val previousDay = messages.getOrNull(index - 1)?.getTimestamp()?.let { dayFormat.format(it) }
                if (day != null && day != previousDay) {
                    Text(
                        day,
                        fontSize = 16.sp,
                        color = colors.darkerGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                }
                ChatBubble(
                    post = post,
                    isOwn = post.user.uname != uname,
                    onLinkClick = onLinkClick,
                    onUserClick = onUserClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(stringResource(R.string.Enter_a_message), color = colors.darkerGray) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                ),
            )
            Spacer(Modifier.width(8.dp))
            val canSend = inputText.isNotBlank()
            IconButton(
                enabled = canSend,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(30),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFDDDDDD),
                    disabledContentColor = Color(0xFF888888),
                ),
                onClick = {
                if (inputText.isNotBlank()) {
                    scope.launch {
                        try {
                            prepend(listOf(App.instance.api.postPm(uname, inputText)))
                            inputText = ""
                            focusManager.clearFocus()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.Send), Modifier.size(20.dp))
            }
        }
    }
}

private val dayFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun ChatBubble(
    post: Post,
    isOwn: Boolean,
    onLinkClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val juick = JuickTheme.colors
    val bgColor = if (isOwn) juick.accent else juick.textBackground
    val textColor = if (isOwn) Color.White else juick.text
    val timeColor = if (isOwn) Color.White.copy(alpha = 0.6f) else juick.darkerGray
    val shape = if (isOwn) RoundedCornerShape(20.dp, 20.dp, 0.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 0.dp)

    val (annotatedText, urlPositions) = remember(post) {
        val body = post.getBody() ?: ""
        val spannable = android.text.SpannableString(body)
        android.text.util.Linkify.addLinks(spannable, android.text.util.Linkify.ALL)
        val urls = mutableListOf<UrlPosition>()
        for (span in spannable.getSpans<android.text.style.URLSpan>()) {
            urls.add(UrlPosition(spannable.getSpanStart(span), spannable.getSpanEnd(span), span.url))
        }
        val annotString = buildAnnotatedString {
            append(body)
            for (url in urls) addStyle(SpanStyle(color = if (isOwn) Color.White else juick.primary, textDecoration = TextDecoration.Underline), url.start, url.end)
        }
        annotString to urls
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isOwn) {
            Spacer(Modifier.width(64.dp))
        } else {
            AsyncImage(
                post.user.avatar, null,
                Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).clickable { onUserClick(post.user.uname) },
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier.weight(1f, fill = false).clip(shape).background(bgColor).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.weight(1f, fill = false),
                onClick = { offset ->
                    urlPositions
                        .firstOrNull { it.start <= offset && offset < it.end }
                        ?.let { onLinkClick(it.url) }
                },
            )
            post.getTimestamp()?.let {
                Text(timeFormat.format(it), style = MaterialTheme.typography.bodyMedium, color = timeColor, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (!isOwn) Spacer(Modifier.width(64.dp))
    }
}
