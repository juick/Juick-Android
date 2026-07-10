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

package com.juick.android.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.juick.R
import com.juick.android.MainActivity
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreen_showsBottomNavigation_withThreeTabs() {
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.Subscriptions)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.Discover)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.PMs)
        ).assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsSearchButton_inTopAppBar() {
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.search)
        ).assertIsDisplayed()
    }

    @Test
    fun mainScreen_defaultStartDestination_isHome() {
        // Home is the start destination — title shows "Juick"
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.Juick)
        ).assertIsDisplayed()
    }
}
