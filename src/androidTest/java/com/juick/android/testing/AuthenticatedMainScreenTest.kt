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

import android.accounts.Account
import android.accounts.AccountManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.juick.R
import com.juick.android.MainActivity
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class AuthenticatedMainScreenTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setupAccount() {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val am = AccountManager.get(ctx)
            am.addAccountExplicitly(
                Account("test", ctx.getString(R.string.applicationId)),
                "test_hash", null
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${ctx.packageName} android.permission.POST_NOTIFICATIONS"
            )
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            AccountManager.get(ctx).getAccountsByType(ctx.getString(R.string.applicationId)).forEach {
                AccountManager.get(ctx).removeAccountExplicitly(it)
            }
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsBottomNavigation_withThreeTabs() {
        val s = composeTestRule.activity
        composeTestRule.onNodeWithText(s.getString(R.string.Subscriptions)).assertIsDisplayed()
        composeTestRule.onNodeWithText(s.getString(R.string.Discover)).assertIsDisplayed()
        composeTestRule.onNodeWithText(s.getString(R.string.PMs)).assertIsDisplayed()
    }

    @Test
    fun showsSearchButton_inTopAppBar() {
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.search)
        ).assertIsDisplayed()
    }
}
