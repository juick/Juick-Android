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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juick.R
import com.juick.android.SignInActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<SignInActivity>()

    @Test
    fun signInScreen_showsLoginButton() {
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.login)
        ).assertIsDisplayed()
    }

    @Test
    fun signInScreen_showsNicknameField_enabled() {
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.your_nickname)
        ).assertIsDisplayed()
    }

    @Test
    fun signInScreen_showsPasswordField() {
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.Password)
        ).assertIsDisplayed()
    }
}
