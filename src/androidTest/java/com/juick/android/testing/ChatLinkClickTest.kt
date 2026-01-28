/*
 * Copyright (C) 2008-2025, Juick
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
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juick.android.MainActivity
import com.juick.android.screens.chat.ChatFragment
import com.juick.api.model.Post
import com.juick.api.model.User
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import org.hamcrest.CoreMatchers
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
        scenario.onActivity { activity ->
            val frag = ChatFragment()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.content, frag, "testChat")
                .commitNow()

            val post = Post(user = User(0, "test")).apply {
                setBody("Check https://example.com")
                mid = 0
                to = null
            }
            val adapter = MessagesListAdapter<Post>("test") { imageView, url, _ -> }
            // set adapter on the MessagesList inside fragment view
            val messagesList = frag.view?.findViewById<MessagesList>(com.juick.R.id.messagesList)
            messagesList?.setAdapter(adapter)
            adapter.addToEnd(listOf(post), true)
            // try to force RecyclerView to create/bind the view
            activity.runOnUiThread {
                messagesList?.scrollToPosition(0)
            }

            // ensure linkify helper ran
            try {
                val method = frag::class.java.getDeclaredMethod("applyLinkifyToVisibleMessages")
                method.isAccessible = true
                method.invoke(frag)
            } catch (_: Exception) {
            }

            // wait for the adapter to create and attach the message view, then find messageText inside it
            // Let Espresso find the created message TextView and click it. Espresso
            // will wait for the view to be present and idle.
        }

        // Use Espresso to click the message text (synchronized)
        Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.withId(com.stfalcon.chatkit.R.id.messageText),
                ViewMatchers.withText("Check https://example.com")
            )
        ).perform(ViewActions.click())

        intended(CoreMatchers.allOf(hasAction(Intent.ACTION_VIEW), hasData("https://example.com")))
    }
}
