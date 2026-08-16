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

import android.R
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juick.android.MainActivity
import com.juick.android.screens.chat.ChatFragment
import com.juick.api.model.Post
import com.juick.api.model.User
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatLinkClickTest {

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun messageLink_opensBrowser_byInvokingURLSpan() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // wait until MainActivity finished its initial navigation, so our
        // fragment is not replaced by async home setup
        onView(withId(com.juick.R.id.main_layout)).check(matches(isDisplayed()))

        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.content, ChatFragment(), "testChat")
                .commitNow()
        }

        // wait until the profile observer installs the production adapter
        onView(allOf(withId(com.juick.R.id.messagesList), withAdapterInstalled()))
            .check(matches(isDisplayed()))

        // add fixture message through the production adapter; the child-attach
        // listener linkifies it
        val post = Post(user = User(0, "test")).apply {
            setBody("Check https://example.com")
            mid = 0
            to = null
        }
        scenario.onActivity { activity ->
            val frag = activity.supportFragmentManager.findFragmentByTag("testChat") as ChatFragment
            val list = frag.view?.findViewById<MessagesList>(com.juick.R.id.messagesList)
            @Suppress("UNCHECKED_CAST")
            (list?.adapter as MessagesListAdapter<Post>).addToEnd(listOf(post), true)
            list.scrollToPosition(0)
        }

        // wait for RecyclerView to bind the message item
        onView(allOf(withId(com.stfalcon.chatkit.R.id.messageText), withText("Check https://example.com")))
            .check(matches(isDisplayed()))

        onView(allOf(withId(com.stfalcon.chatkit.R.id.messageText), withText("Check https://example.com")))
            .perform(click())

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://example.com")))
    }

    private fun withAdapterInstalled(): Matcher<View> = object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View) =
            (view as? MessagesList)?.adapter is MessagesListAdapter<*>

        override fun describeTo(description: Description) {
            description.appendText("MessagesList with production adapter installed")
        }
    }
}
