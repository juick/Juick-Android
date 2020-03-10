/*
 * Copyright (C) 2008-2020, Juick
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

package com.juick.util;

import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.juick.android.MainActivity;
import com.juick.api.model.Post;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    private static final Pattern urlPattern = Pattern.compile("((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
    private static final DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat outDateFormat = new SimpleDateFormat("HH:mm dd MMM yyyy");

    static {
        outDateFormat.setTimeZone(TimeZone.getDefault());
        sourceDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    public static String formatMessageTimestamp(Post jmsg) {
        return outDateFormat.format(jmsg.getTimestamp());
    }


    // TODO: taken from juick-core, need merge

    private static Pattern regexLinks2 = Pattern.compile("((?<=\\s)|(?<=\\A))([\\[\\{]|&lt;)((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\\"\\)\\!]+)/?(?:[^\\]\\}](?<!&gt;))*)([\\]\\}]|&gt;)");

    public static String formatMessage(String msg) {
        msg = msg.replaceAll("&", "&amp;");
        msg = msg.replaceAll("<", "&lt;");
        msg = msg.replaceAll(">", "&gt;");

        // --
        // &mdash;
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))\\-\\-?((?=\\s)|(?=\\Z))", "$1&mdash;$2");

        // http://juick.com/last?page=2
        // <a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\n\\\"]+)/?[^\\s\\n\\\"]*)", "$1<a href=\"$2\" rel=\"nofollow\">$3</a>");

        // [link text][http://juick.com/last?page=2]
        // <a href="http://juick.com/last?page=2" rel="nofollow">link text</a>
        msg = msg.replaceAll("\\[([^\\]]+)\\]\\[((?:ht|f)tps?://[^\\]]+)\\]", "<a href=\"$2\" rel=\"nofollow\">$1</a>");
        msg = msg.replaceAll("\\[([^\\]]+)\\]\\(((?:ht|f)tps?://[^\\)]+)\\)", "<a href=\"$2\" rel=\"nofollow\">$1</a>");

        // #12345
        // <a href="http://juick.com/12345">#12345</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)((?=\\s)|(?=\\Z)|(?=\\))|(?=\\.)|(?=\\,))", "$1<a href=\"https://juick.com/thread/$2\">#$2</a>$3");

        // #12345/65
        // <a href="http://juick.com/12345#65">#12345/65</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"https://juick.com/thread/$2#$3\">#$2/$3</a>$4");

        // *bold*
        // <b>bold</b>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))\\*([^\\*\\n<>]+)\\*((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<b>$2</b>$3");

        // /italic/
        // <i>italic</i>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))/([^\\/\\n<>]+)/((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<i>$2</i>$3");

        // _underline_
        // <span class="u">underline</span>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))_([^\\_\\n<>]+)_((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<span class=\"u\">$2</span>$3");

        // /12
        // <a href="#12">/12</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))\\/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"#$2\">/$2</a>$3");

        // @username@jabber.org
        // <a href="http://juick.com/username@jabber.org/">@username@jabber.org</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))@([\\w\\-\\.]+@[\\w\\-\\.]+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"https://juick.com/$2/\">@$2</a>$3");

        // @username
        // <a href="http://juick.com/username/">@username</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))@([\\w\\-]{2,16})((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"https://juick.com/$2/\">@$2</a>$3");

        // (http://juick.com/last?page=2)
        // (<a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>)
        Matcher m = regexLinks2.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(3).replace(" ", "%20").replaceAll("\\s+", StringUtils.EMPTY);
            m.appendReplacement(sb, "$1$2<a href=\"" + url + "\" rel=\"nofollow\">$4</a>$5");
        }
        m.appendTail(sb);
        msg = sb.toString();

        // > citate
        msg = msg.replaceAll("(?:(?<=\\n)|(?<=\\A))&gt; *(.*)?(\\n|(?=\\Z))", "<q>$1</q>");
        msg = msg.replaceAll("</q><q>", "\n");

        msg = msg.replaceAll("\n", "<br/>\n");
        return msg;
    }
}
