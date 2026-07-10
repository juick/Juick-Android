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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.juick.android.ui.screens.feed.formatPostText
import com.juick.api.model.Post
import com.juick.api.model.User
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormatPostTextTest {

    private val primary = Color(0xFF2A6090)
    private val dimmed = Color(0xFF6D6D6D)
    private val onSurface = Color(0xFF222222)
    private val quote = Color(0xFF666666)

    @Test
    fun plainMessage_rendersCleanly() {
        val post = post("Hello world")
        assertThat(formatPostText(post, primary, dimmed, onSurface).text).contains("Hello world")
    }

    @Test
    fun tags_rendersWithHashPrefix() {
        val post = post("Check", tags = listOf("test", "android"))
        val result = formatPostText(post, primary, dimmed, onSurface)
        assertThat(result.text).contains("#test")
        assertThat(result.text).contains("#android")
    }

    @Test
    fun tagsUseDimmedColor() {
        val post = post("", tags = listOf("mytag"))
        val result = formatPostText(post, primary, dimmed, onSurface)
        val tagStart = result.text.indexOf("#mytag")
        val tagEnd = tagStart + "#mytag".length
        val hasDimmed = result.spanStyles.any { s ->
            s.start <= tagStart && s.end >= tagEnd && s.item.color == dimmed
        }
        assertThat(hasDimmed).isTrue()
    }

    @Test
    fun linkEntity_usesPrimaryColor() {
        val post = post(
            body = "Check example.com now",
            entities = listOf(e("a", 6, 17, "https://example.com"))
        )
        val result = formatPostText(post, primary, dimmed, onSurface)
        assertThat(result.text).contains("example.com")
        // Link text should have primary color
        val linkStart = result.text.indexOf("example.com")
        val hasLink = result.spanStyles.any { s ->
            s.start <= linkStart && s.end >= linkStart + "example.com".length && s.item.color == primary
        }
        assertThat(hasLink).isTrue()
    }

    @Test
    fun quoteEntity_usesQuoteColor() {
        val post = post(
            body = "quoted text",
            entities = listOf(Post.Entity(0, 11, "quoted text", "q"))
        )
        val result = formatPostText(post, primary, dimmed, onSurface, quote)
        val hasQuote = result.spanStyles.any { s -> s.item.color == quote }
        assertThat(hasQuote).isTrue()
    }

    @Test
    fun bodyTextOutsideEntities_usesOnSurfaceColor() {
        val post = post(
            body = "normal text https://example.com more",
            entities = listOf(e("a", 12, 31, "https://example.com"))
        )
        val result = formatPostText(post, primary, dimmed, onSurface)
        assertThat(result.text).contains("normal text")
        assertThat(result.text).contains("more")
    }

    @Test
    fun emptyBody_returnsTagsOnly() {
        val post = post("", tags = listOf("test"))
        val result = formatPostText(post, primary, dimmed, onSurface)
        assertThat(result.text.trim()).isEqualTo("#test")
    }

    @Test
    fun entitiesIgnored_whenPositionsOutsideBody() {
        val post = post(
            body = "short",
            entities = listOf(e("a", 100, 200, "https://x.com"))
        )
        val result = formatPostText(post, primary, dimmed, onSurface)
        assertThat(result.text).contains("short")
    }

    // Helpers
    private fun post(body: String, tags: List<String> = emptyList(), entities: List<Post.Entity> = emptyList()): Post {
        return Post(User(0, "test")).apply { setBody(body); this.tags = tags; this.entities = entities }
    }

    private fun e(type: String, start: Int, end: Int, url: String) =
        Post.Entity(start, end, url, type, url)
}
