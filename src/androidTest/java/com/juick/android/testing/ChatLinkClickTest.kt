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

        val post = Post(user = User(0, "test")).apply {
            setBody("Check https://example.com")
            mid = 0
            to = null
        }

        scenario.onActivity { activity ->
            val frag = ChatFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.content, frag, "testChat")
                .commitNow()
        }

        // chat fragment must be attached before we touch its list
        onView(withId(com.juick.R.id.messagesList)).check(matches(isDisplayed()))

        scenario.onActivity { activity ->
            val frag = activity.supportFragmentManager.findFragmentByTag("testChat") as ChatFragment
            val messagesList = frag.view?.findViewById<MessagesList>(com.juick.R.id.messagesList)
            val adapter = MessagesListAdapter<Post>("test") { _, _, _ -> }
            messagesList?.setAdapter(adapter)
            adapter.addToEnd(listOf(post), true)
            messagesList?.scrollToPosition(0)
        }

        // wait for RecyclerView to bind the message item
        onView(allOf(withId(com.stfalcon.chatkit.R.id.messageText), withText("Check https://example.com")))
            .check(matches(isDisplayed()))

        // linkify after the item is bound, so links are actually clickable
        scenario.onActivity { activity ->
            val frag = activity.supportFragmentManager.findFragmentByTag("testChat") as ChatFragment
            val method = frag.javaClass.getDeclaredMethod("applyLinkifyToVisibleMessages")
            method.isAccessible = true
            method.invoke(frag)
        }

        onView(allOf(withId(com.stfalcon.chatkit.R.id.messageText), withText("Check https://example.com")))
            .perform(click())

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://example.com")))
    }
}
