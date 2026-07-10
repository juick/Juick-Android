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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.juick.android.ui.screens.feed.PostCard
import com.juick.android.ui.screens.feed.buildUrlPositions
import com.juick.android.ui.screens.feed.formatPostText
import com.juick.api.model.Post
import com.juick.api.model.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkClickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val primary = Color(0xFF2A6090)
    private val dimmed = Color(0xFF6D6D6D)
    private val onSurface = Color(0xFF222222)

    @Test
    fun formattedPost_withLink_usesPrimaryColor() {
        val post = Post(User(0, "test")).apply {
            setBody("Check example.com now")
            entities = listOf(e(6, 17, "https://example.com"))
            mid = 1
        }
        val annotated = formatPostText(post, primary, dimmed, onSurface)
        assertThat(annotated.text).contains("example.com")
        val linkStart = annotated.text.indexOf("example.com")
        val hasLinkStyle = annotated.spanStyles.any { s ->
            s.start <= linkStart && s.end >= linkStart + "example.com".length && s.item.color == primary
        }
        assertThat(hasLinkStyle).isTrue()
    }

    @Test
    fun postCard_rendersLinkText_visible() {
        val post = Post(User(0, "test")).apply {
            setBody("Check example.com")
            entities = listOf(e(6, 17, "https://example.com"))
            mid = 1
        }
        composeTestRule.setContent { PostCard(post, {}, {}, {}, {}, {}) }
        composeTestRule.onNodeWithText("example.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun buildUrlPositions_extractsLinkFromEntities() {
        val post = Post(User(0, "test")).apply {
            setBody("Click juick.com now")
            entities = listOf(e(6, 15, "https://juick.com/m/12345"))
            mid = 2
        }
        val urls = buildUrlPositions(post)
        assertThat(urls).hasSize(1)
        assertThat(urls[0].url).isEqualTo("https://juick.com/m/12345")
    }

    @Test
    fun buildUrlPositions_skipsNonLinkEntities() {
        val post = Post(User(0, "test")).apply {
            setBody("text link quote")
            entities = listOf(
                e(5, 9, "https://a.com"),
                Post.Entity(10, 15, "quote", "q"),
            )
            mid = 3
        }
        val urls = buildUrlPositions(post)
        assertThat(urls).hasSize(1)
        assertThat(urls[0].url).isEqualTo("https://a.com")
    }

    @Test
    fun postCard_withTagsAndLink_rendersBoth() {
        val post = Post(User(0, "test")).apply {
            setBody("Check example.com")
            tags = listOf("cool")
            entities = listOf(e(6, 17, "https://example.com"))
            mid = 4
        }
        val annotated = formatPostText(post, primary, dimmed, onSurface)
        assertThat(annotated.text).contains("#cool")
        assertThat(annotated.text).contains("example.com")
    }

    private fun e(start: Int, end: Int, url: String) =
        Post.Entity(start, end, url, "a", url)
}
