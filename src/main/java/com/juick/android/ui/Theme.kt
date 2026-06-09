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
package com.juick.android.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val JuickLightColors = lightColorScheme(
    primary = Color(0xFF2A6090),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E4F2),
    onPrimaryContainer = Color(0xFF0A1F30),
    secondary = Color(0xFF5D6B62),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F0E5),
    onSecondaryContainer = Color(0xFF1A2820),
    tertiary = Color(0xFFFF339A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD1E5),
    onTertiaryContainer = Color(0xFF4D0026),
    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF222222),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF222222),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF6D6D6D),
    outline = Color(0xFFDEDEDE),
    outlineVariant = Color(0xFFEEEEEE),
    scrim = Color(0xFF000000),
)

private val JuickDarkColors = darkColorScheme(
    primary = Color(0xFFD4A56A),
    onPrimary = Color(0xFF3A2510),
    primaryContainer = Color(0xFF5A3A15),
    onPrimaryContainer = Color(0xFFFFDCC5),
    secondary = Color(0xFF9EAE9E),
    onSecondary = Color(0xFF1A2820),
    secondaryContainer = Color(0xFF3A4A3A),
    onSecondaryContainer = Color(0xFFD0E0D0),
    tertiary = Color(0xFFFF5CA8),
    onTertiary = Color(0xFF4D0026),
    tertiaryContainer = Color(0xFF800040),
    onTertiaryContainer = Color(0xFFFFD1E5),
    background = Color(0xFF333333),
    onBackground = Color(0xFFCCCCCC),
    surface = Color(0xFF383838),
    onSurface = Color(0xFFCCCCCC),
    surfaceVariant = Color(0xFF4C4C4C),
    onSurfaceVariant = Color(0xFFA2A2A2),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF4C4C4C),
    scrim = Color(0xFF000000),
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) JuickDarkColors else JuickLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

val standardSpacing = 16.dp
