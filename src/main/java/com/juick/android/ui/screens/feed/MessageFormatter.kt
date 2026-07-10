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
package com.juick.android.ui.screens.feed

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.juick.api.model.Post

private data class Processed(
    val text: String,
    val entityStart: List<Int>,
    val entityEnd: List<Int>,
    val entityType: List<String>,
)

private fun StringBuilder.appendCollapsing(ch: Char) {
    if (ch != '\n' || isEmpty() || last() != '\n') append(ch)
}

private fun processBody(post: Post): Processed {
    val body = post.getBody() ?: ""
    val sorted = post.entities.sortedBy { it.start }

    val sb = StringBuilder()
    val eStart = mutableListOf<Int>()
    val eEnd = mutableListOf<Int>()
    val eType = mutableListOf<String>()
    var bp = 0

    for (e in sorted) {
        if (e.start < bp) continue
        val end = e.end.coerceAtMost(body.length)
        while (bp < body.length && bp < e.start) sb.appendCollapsing(body[bp++])
        eStart.add(sb.length)
        for (c in e.text) sb.appendCollapsing(c)
        eEnd.add(sb.length)
        eType.add(e.type)
        bp = end
    }
    while (bp < body.length) sb.appendCollapsing(body[bp++])

    val raw = sb.toString()
    val trimmed = raw.trim('\n', ' ')
    val trimOffset = raw.indexOf(trimmed)
    val adjustedStart = eStart.map { (it - trimOffset).coerceIn(0, trimmed.length) }
    val adjustedEnd = eEnd.map { (it - trimOffset).coerceIn(0, trimmed.length) }
    return Processed(trimmed, adjustedStart, adjustedEnd, eType)
}

fun formatPostText(
    post: Post,
    primaryColor: Color,
    dimmedColor: Color,
    onSurfaceColor: Color,
    quoteColor: Color = dimmedColor,
): AnnotatedString {
    val p = processBody(post)
    return buildAnnotatedString {
        for (tag in post.tags) {
            withStyle(SpanStyle(color = dimmedColor)) { append("#$tag ") }
        }
        var pos = 0
        for (i in p.entityStart.indices) {
            if (pos < p.entityStart[i]) {
                withStyle(SpanStyle(color = onSurfaceColor)) { append(p.text, pos, p.entityStart[i]) }
            }
            val style = entityStyle(p.entityType[i], primaryColor, dimmedColor, onSurfaceColor, quoteColor)
            withStyle(style) { append(p.text, p.entityStart[i], p.entityEnd[i]) }
            pos = p.entityEnd[i]
        }
        if (pos < p.text.length) {
            withStyle(SpanStyle(color = onSurfaceColor)) { append(p.text, pos, p.text.length) }
        }
    }
}

fun formatPostBlocks(
    post: Post,
    primaryColor: Color,
    dimmedColor: Color,
    onSurfaceColor: Color,
    quoteColor: Color = dimmedColor,
): List<TextBlock> {
    val p = processBody(post)
    val sorted = post.entities.sortedBy { it.start }

    data class Seg(val start: Int, val end: Int, val type: String)

    val segs = mutableListOf<Seg>()
    var ei = 0
    var lastAcceptedEnd = 0
    for (i in p.entityStart.indices) {
        val e = sorted.getOrNull(ei) ?: break
        if (e.start < lastAcceptedEnd) { ei++; continue }
        segs.add(Seg(p.entityStart[i], p.entityEnd[i], p.entityType[i]))
        lastAcceptedEnd = e.end
        ei++
    }

    val all = mutableListOf<Seg>()
    var pos = 0
    for (s in segs) {
        if (pos < s.start) all.add(Seg(pos, s.start, ""))
        all.add(s)
        pos = s.end
    }
    if (pos < p.text.length) all.add(Seg(pos, p.text.length, ""))

    val blocks = mutableListOf<TextBlock>()
    var tagsRendered = false
    var i = 0

    if (all.isNotEmpty() && all[0].type == "q") {
        val tBuilder = buildAnnotatedString {
            for (tag in post.tags) withStyle(SpanStyle(color = dimmedColor)) { append("#$tag ") }
        }
        if (post.tags.isNotEmpty()) blocks.add(TextBlock.Regular(tBuilder, emptyList()))
        tagsRendered = true
    }

    while (i < all.size) {
        if (all[i].type == "q") {
            val startPos = all[i].start
            var endPos = all[i].end
            i++
            while (i < all.size) {
                if (all[i].type == "q") { endPos = all[i].end; i++ }
                else if (all[i].type == "" && i + 1 < all.size && all[i + 1].type == "q") {
                    val gapLen = all[i].end - all[i].start
                    if (gapLen <= 2) { endPos = all[i].end; i++ } else break
                } else break
            }
            val qBuilder = buildAnnotatedString {
                withStyle(SpanStyle(color = quoteColor)) { append(p.text, startPos, endPos) }
            }
            blocks.add(TextBlock.Quote(qBuilder, emptyList()))
        } else {
            val rBuilder = buildAnnotatedString {
                if (!tagsRendered) {
                    for (tag in post.tags) withStyle(SpanStyle(color = dimmedColor)) { append("#$tag ") }
                    tagsRendered = true
                }
                while (i < all.size && all[i].type != "q") {
                    val s = all[i]
                    val style = if (s.type.isEmpty()) SpanStyle(color = onSurfaceColor)
                    else entityStyle(s.type, primaryColor, dimmedColor, onSurfaceColor, quoteColor)
                    withStyle(style) { append(p.text, s.start, s.end) }
                    i++
                }
            }
            val rText = rBuilder.text
            if (rText.isNotEmpty()) {
                val rUrls = sorted.mapNotNull { e ->
                    if (e.type != "a" || e.url == null) return@mapNotNull null
                    val idx = rText.indexOf(e.text)
                    if (idx >= 0) UrlPosition(idx, idx + e.text.length, e.url) else null
                }
                blocks.add(TextBlock.Regular(rBuilder, rUrls))
            }
        }
    }
    if (blocks.isEmpty() && post.tags.isNotEmpty()) {
        val t = buildAnnotatedString { for (tag in post.tags) withStyle(SpanStyle(color = dimmedColor)) { append("#$tag ") } }
        blocks.add(TextBlock.Regular(t, emptyList()))
    }
    return blocks
}

private fun entityStyle(type: String, primary: Color, dimmed: Color, onSurface: Color, quote: Color = dimmed): SpanStyle = when (type) {
    "a" -> SpanStyle(color = primary)
    "q" -> SpanStyle(color = quote)
    "b" -> SpanStyle(fontWeight = FontWeight.Bold)
    "i" -> SpanStyle(fontStyle = FontStyle.Italic)
    "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
    else -> SpanStyle(color = onSurface)
}

fun buildUrlPositions(post: Post): List<UrlPosition> {
    val p = processBody(post)
    val sorted = post.entities.sortedBy { it.start }
    var si = 0
    return p.entityStart.indices.mapNotNull { i ->
        while (si < sorted.size && sorted[si].type != "a") si++
        if (si >= sorted.size) return@mapNotNull null
        val e = sorted[si++]
        if (e.url == null) return@mapNotNull null
        UrlPosition(p.entityStart[i], p.entityEnd[i], e.url)
    }
}

sealed class TextBlock {
    data class Regular(val annotatedString: AnnotatedString, val urlPositions: List<UrlPosition>) : TextBlock()
    data class Quote(val annotatedString: AnnotatedString, val urlPositions: List<UrlPosition>) : TextBlock()
}

data class UrlPosition(val start: Int, val end: Int, val url: String)
