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
 */

package com.juick.android.ui.screens.feed

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.juick.R
import com.juick.api.model.Post

@Composable
fun LikeConfirmDialog(post: Post, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val removing = post.liked
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_ei_heart), null) },
        title = { Text(stringResource(if (removing) R.string.Remove_recommendation else R.string.Recommend_post)) },
        text = {
            Text(
                if (removing) stringResource(R.string.recommendation_will_be_removed)
                else stringResource(R.string.recommendation_will_be_public, post.user.uname)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(if (removing) R.string.remove else R.string.recommend))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.Cancel)) }
        },
    )
}
