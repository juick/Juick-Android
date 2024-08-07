/*
 * Copyright (C) 2008-2024, Juick
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

import android.text.style.ClickableSpan
import android.text.style.QuoteSpan
import androidx.core.text.getSpans
import androidx.core.text.toSpanned
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.juick.App
import com.juick.R
import com.juick.android.MainActivity
import com.juick.android.screens.FeedAdapter
import com.juick.api.model.Post
import com.juick.util.getString
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class UITest {

    @Test
    fun isDisplayed_MainActivity() {
        launchActivity<MainActivity>().use {
            onView(withId(R.id.main_layout))
                .check(matches(isDisplayed()))
        }
    }
    @Test
    fun isCorrectNotification_NotificationSender() {
        assumeTrue("UIAutomator tests require API18", android.os.Build.VERSION.SDK_INT >= 18)
        val notificationData = this.javaClass.getResourceAsStream("/test_notification.json")
        val notificationJson = App.instance.jsonMapper.parseToJsonElement(notificationData?.getString() ?: "")
        App.instance.notificationSender.showNotification(notificationJson.toString())
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        device.wait(Until.hasObject(By.textStartsWith("Hello, world!")), 5000)
        device.pressHome()
    }

    @Test
    fun formatMessage_spans() {
        val msg = "> quote\n> quote 2\n" +
                "text [link](http://ya.ru)"
        val post = Post.empty()
        post.tags = arrayListOf("tag")
        post.setBody(msg)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val formatted = FeedAdapter.formatMessageText(appContext, post).toSpanned()
        assertThat(formatted.getSpans<QuoteSpan>().size).isEqualTo(1)
        assertThat(formatted.getSpans<ClickableSpan>().size).isEqualTo(2)
    }
}
