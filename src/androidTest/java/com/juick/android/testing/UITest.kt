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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.juick.App
import com.juick.android.MainActivity
import com.juick.android.NotificationSender
import com.juick.android.ui.screens.feed.formatPostText
import com.juick.api.model.Post
import com.juick.api.model.User
import com.juick.util.getString
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class UITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun isDisplayed_MainActivity() {
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun isCorrectNotification_NotificationSender() {
        val notificationData = this.javaClass.getResourceAsStream("/test_notification.json")
        val notificationJson = App.instance.jsonMapper.parseToJsonElement(notificationData?.getString() ?: "")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        NotificationSender.showNotification(context, notificationJson.toString())
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        device.wait(Until.hasObject(By.textStartsWith("Hello, world!")), 5000)
        device.pressHome()
    }

    @Test
    fun formatMessage_spans() {
        val post = Post(User(0, "test")).apply {
            tags = arrayListOf("tag")
            setBody("> quote\n> quote 2\ntext link")
            entities = listOf(
                Post.Entity(2, 7, "quote", "q"),
                Post.Entity(10, 15, "quote 2", "q"),
                Post.Entity(21, 25, "link", "a", "http://ya.ru"),
            )
        }
        val primary = Color(0xFF2A6090)
        val dimmed = Color(0xFF6D6D6D)
        val onSurface = Color(0xFF222222)
        val quote = Color(0xFF666666)
        val formatted = formatPostText(post, primary, dimmed, onSurface, quote)

        assertThat(formatted.text).contains("quote")
        assertThat(formatted.text).contains("link")
        assertThat(formatted.text).contains("#tag")

        val linkStart = formatted.text.indexOf("link")
        val hasLinkStyle = formatted.spanStyles.any { it.start <= linkStart && it.end >= linkStart + 4 && it.item.color == primary }
        assertThat(hasLinkStyle).isTrue()

        val quoteStart = formatted.text.indexOf("quote")
        val hasQuoteStyle = formatted.spanStyles.any { it.start <= quoteStart && it.end >= quoteStart + 5 && it.item.color == quote }
        assertThat(hasQuoteStyle).isTrue()
    }
}
