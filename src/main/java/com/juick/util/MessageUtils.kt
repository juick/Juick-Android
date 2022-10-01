/*
 * Copyright (C) 2008-2022, Juick
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
package com.juick.util

import com.juick.api.model.Post
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object MessageUtils {
    private val urlPattern = Pattern.compile(
        "((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*",
        Pattern.CASE_INSENSITIVE
    )
    private val sourceDateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val outDateFormat: DateFormat = SimpleDateFormat("HH:mm dd MMM yyyy")

    init {
        outDateFormat.timeZone = TimeZone.getDefault()
        sourceDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun formatMessageTimestamp(jmsg: Post): String {
        return outDateFormat.format(jmsg.getTimestamp())
    }

    // TODO: taken from juick-core, need merge
    private val regexLinks2 =
        Pattern.compile("((?<=\\s)|(?<=\\A))([\\[\\{]|&lt;)((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\\"\\)\\!]+)/?(?:[^\\]\\}](?<!&gt;))*)([\\]\\}]|&gt;)")

    fun formatMessage(msg: String): String {
        var msg = msg
        msg = msg.replace("&".toRegex(), "&amp;")
        msg = msg.replace("<".toRegex(), "&lt;")
        msg = msg.replace(">".toRegex(), "&gt;")

        // --
        // &mdash;
        msg = msg.replace("((?<=\\s)|(?<=\\A))\\-\\-?((?=\\s)|(?=\\Z))".toRegex(), "$1&mdash;$2")

        // http://juick.com/last?page=2
        // <a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A))((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\n\\\"]+)/?[^\\s\\n\\\"]*)".toRegex(),
            "$1<a href=\"$2\" rel=\"nofollow\">$3</a>"
        )

        // [link text][http://juick.com/last?page=2]
        // <a href="http://juick.com/last?page=2" rel="nofollow">link text</a>
        msg = msg.replace(
            "\\[([^\\]]+)\\]\\[((?:ht|f)tps?://[^\\]]+)\\]".toRegex(),
            "<a href=\"$2\" rel=\"nofollow\">$1</a>"
        )
        msg = msg.replace(
            "\\[([^\\]]+)\\]\\(((?:ht|f)tps?://[^\\)]+)\\)".toRegex(),
            "<a href=\"$2\" rel=\"nofollow\">$1</a>"
        )

        // #12345
        // <a href="http://juick.com/12345">#12345</a>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)((?=\\s)|(?=\\Z)|(?=\\))|(?=\\.)|(?=\\,))".toRegex(),
            "$1<a href=\"https://juick.com/thread/$2\">#$2</a>$3"
        )

        // #12345/65
        // <a href="http://juick.com/12345#65">#12345/65</a>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<a href=\"https://juick.com/thread/$2#$3\">#$2/$3</a>$4"
        )

        // *bold*
        // <b>bold</b>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))\\*([^\\*\\n<>]+)\\*((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<b>$2</b>$3"
        )

        // /italic/
        // <i>italic</i>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A))/([^\\/\\n<>]+)/((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<i>$2</i>$3"
        )

        // _underline_
        // <span class="u">underline</span>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A))_([^\\_\\n<>]+)_((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<span class=\"u\">$2</span>$3"
        )

        // /12
        // <a href="#12">/12</a>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A))\\/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<a href=\"#$2\">/$2</a>$3"
        )

        // @username@jabber.org
        // <a href="http://juick.com/username@jabber.org/">@username@jabber.org</a>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A))@([\\w\\-\\.]+@[\\w\\-\\.]+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<a href=\"https://juick.com/$2/\">@$2</a>$3"
        )

        // @username
        // <a href="http://juick.com/username/">@username</a>
        msg = msg.replace(
            "((?<=\\s)|(?<=\\A))@([\\w\\-]{2,16})((?=\\s)|(?=\\Z)|(?=\\p{Punct}))".toRegex(),
            "$1<a href=\"https://juick.com/$2/\">@$2</a>$3"
        )

        // (http://juick.com/last?page=2)
        // (<a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>)
        val m = regexLinks2.matcher(msg)
        val sb = StringBuffer()
        while (m.find()) {
            val url = m.group(3).replace(" ", "%20").replace("\\s+".toRegex(), StringUtils.EMPTY)
            m.appendReplacement(sb, "$1$2<a href=\"$url\" rel=\"nofollow\">$4</a>$5")
        }
        m.appendTail(sb)
        msg = sb.toString()

        // > citate
        msg = msg.replace("(?:(?<=\\n)|(?<=\\A))&gt; *(.*)?(\\n|(?=\\Z))".toRegex(), "<q>$1</q>")
        msg = msg.replace("</q><q>".toRegex(), "\n")
        msg = msg.replace("\n".toRegex(), "<br/>\n")
        return msg
    }

    fun haveNSFWContent(post: Post): Boolean {
        return (post.tags.contains("NSFW")
                || post.tags.contains("girlz")
                || post.tags.contains("сиськи"))
    }
}