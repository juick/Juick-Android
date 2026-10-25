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
package com.juick.android.ui.screens.post

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.juick.R
import com.juick.android.ui.AvatarShape
import com.juick.android.ui.CardShape
import com.juick.android.ui.JuickDivider
import com.juick.android.ui.JuickTheme
import com.juick.android.ui.screens.feed.formatPostText
import com.juick.api.model.Post
import com.juick.util.MessageUtils

@Composable
fun Composer(
    title: String,
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    attachment: Uri?,
    onAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    sendEnabled: Boolean,
    isSending: Boolean,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onTagsClick: (() -> Unit)? = null,
    replyTo: Post? = null,
) {
    val colors = JuickTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.Cancel), color = colors.primary, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onTagsClick != null) {
                IconButton(onClick = onTagsClick, enabled = !isSending) {
                    Icon(painterResource(R.drawable.ic_code_tags_black_24dp), stringResource(R.string.tags_button), tint = colors.primary)
                }
            }
            IconButton(onClick = onAttachClick, enabled = !isSending) {
                Icon(
                    painterResource(R.drawable.ic_attach_file_black_24dp),
                    stringResource(R.string.attach_photo),
                    tint = if (attachment != null) colors.accent else colors.primary,
                )
            }
            Button(
                onClick = onSend,
                enabled = sendEnabled && !isSending,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = colors.feed,
                    disabledContentColor = colors.dimmed,
                ),
                modifier = Modifier.padding(end = 8.dp).height(36.dp),
            ) {
                Text(stringResource(R.string.Send), fontWeight = FontWeight.SemiBold)
            }
        }
        if (isSending) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.border)
        } else {
            JuickDivider()
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            replyTo?.let { ReplyPreview(it) }
            attachment?.let { uri ->
                Box(Modifier.size(100.dp)) {
                    AsyncImage(
                        uri, stringResource(R.string.Attach),
                        Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.feed),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .clickable(onClick = onRemoveAttachment),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Close, stringResource(R.string.Cancel), Modifier.size(14.dp), tint = Color.White)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text, lineHeight = 23.sp),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                )
                if (text.text.isEmpty()) {
                    Text(stringResource(R.string.Enter_a_message), style = MaterialTheme.typography.bodyLarge, color = colors.dimmed)
                }
            }
        }
    }
}

@Composable
private fun ReplyPreview(post: Post) {
    val colors = JuickTheme.colors
    val body = remember(post, colors) { formatPostText(post, colors.primary, colors.dimmed, colors.text, colors.quote) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .clip(CardShape)
            .background(colors.textBackground)
            .border(1.dp, colors.border, CardShape)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                post.user.avatar, null,
                Modifier.size(28.dp).clip(AvatarShape).background(colors.feed),
                placeholder = painterResource(R.drawable.av_96),
                error = painterResource(R.drawable.av_96),
                contentScale = ContentScale.Crop,
            )
            Text(post.user.uname, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.primary)
            Text(MessageUtils.formatMessageTimestamp(post), style = MaterialTheme.typography.bodySmall, color = colors.dimmed)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium.copy(color = colors.text, lineHeight = 20.sp))
    }
}
