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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class JuickColors(
    val primary: Color,
    val accent: Color,
    val text: Color,
    val dimmed: Color,
    val mainBackground: Color,
    val textBackground: Color,
    val border: Color,
    val darkerGray: Color = Color(0xFFAAAAAA),
    val premium: Color = Color(0xFF669900),
)

private val LightJuickColors = JuickColors(
    primary = Color(0xFF3C77AA),
    accent = Color(0xFFFF339A),
    text = Color(0xFF222222),
    dimmed = Color(0xFF88859D),
    mainBackground = Color(0xFFF8F8F8),
    textBackground = Color(0xFFFFFFFF),
    border = Color(0xFFEEEEEE),
)

private val DarkJuickColors = JuickColors(
    primary = Color(0xFFC38855),
    accent = Color(0xFFFF339A),
    text = Color(0xFFCCCCCC),
    dimmed = Color(0xFF88859D),
    mainBackground = Color(0xFF333333),
    textBackground = Color(0xFF383838),
    border = Color(0xFF4C4C4C),
)

private val LocalJuickColors = staticCompositionLocalOf { LightJuickColors }

object JuickTheme {
    val colors: JuickColors
        @Composable @ReadOnlyComposable get() = LocalJuickColors.current
}

private fun JuickColors.lightScheme() = lightColorScheme(
    primary = primary,
    background = mainBackground,
    onBackground = text,
    surface = mainBackground,
    onSurface = text,
)

private fun JuickColors.darkScheme() = darkColorScheme(
    primary = primary,
    background = mainBackground,
    onBackground = text,
    surface = mainBackground,
    onSurface = text,
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val juickColors = if (useDarkTheme) DarkJuickColors else LightJuickColors
    val colorScheme = if (useDarkTheme) juickColors.darkScheme() else juickColors.lightScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = juickColors.mainBackground.toArgb()
            window.navigationBarColor = juickColors.mainBackground.toArgb()
            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalJuickColors provides juickColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

val standardSpacing = 16.dp
