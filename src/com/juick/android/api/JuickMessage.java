/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android.api;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessage {

    public int MID = 0;
    public int RID = 0;
    public String Text = null;
    public JuickUser User = null;
    public Vector<String> tags = new Vector<String>();
    public Date Timestamp = null;
    public int replies = 0;
    public String Photo = null;
    public String Video = null;

    public static JuickMessage parseJSON(JSONObject json) throws JSONException {
        JuickMessage jmsg = new JuickMessage();
        if (json.has("mid")) {
            jmsg.MID = json.getInt("mid");
        }
        if (json.has("rid")) {
            jmsg.RID = json.getInt("rid");
        }
        if (json.has("body")) {
            jmsg.Text = json.getString("body").replace("&quot;", "\"");
        }
        if (json.has("user")) {
            jmsg.User = JuickUser.parseJSON(json.getJSONObject("user"));
        }

        if (json.has("timestamp")) {
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                jmsg.Timestamp = df.parse(json.getString("timestamp"));
            } catch (ParseException e) {
            }
        }

        if (json.has("tags")) {
            JSONArray tags = json.getJSONArray("tags");
            for (int n = 0; n < tags.length(); n++) {
                jmsg.tags.add(tags.getString(n).replace("&quot;", "\""));
            }
        }

        if (json.has("replies")) {
            jmsg.replies = json.getInt("replies");
        }

        if (json.has("photo")) {
            jmsg.Photo = json.getJSONObject("photo").getString("small");
        }
        if (json.has("video")) {
            jmsg.Video = json.getJSONObject("video").getString("mp4");
        }

        return jmsg;
    }

    public String getTags() {
        String t = new String();
        for (Enumeration e = tags.elements(); e.hasMoreElements();) {
            String tag = (String) e.nextElement();
            if (!t.isEmpty()) {
                t += ' ';
            }
            t += '*' + tag;
        }
        return t;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JuickMessage)) {
            return false;
        }
        JuickMessage jmsg = (JuickMessage) obj;
        return (this.MID == jmsg.MID && this.RID == jmsg.RID);
    }

    public int compareTo(Object obj) throws ClassCastException {
        if (!(obj instanceof JuickMessage)) {
            throw new ClassCastException();
        }
        JuickMessage jmsg = (JuickMessage) obj;

        if (this.MID != jmsg.MID) {
            if (this.MID > jmsg.MID) {
                return -1;
            } else {
                return 1;
            }
        }

        if (this.RID != jmsg.RID) {
            if (this.RID < jmsg.RID) {
                return -1;
            } else {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        String msg = "";
        if (User != null) {
            msg += "@" + User.UName + ": ";
        }
        msg += getTags();
        if (msg.length() > 0) {
            msg += "\n";
        }
        if (Photo != null) {
            msg += Photo + "\n";
        } else if (Video != null) {
            msg += Video + "\n";
        }
        if (Text != null) {
            msg += Text + "\n";
        }
        msg += "#" + MID;
        if (RID > 0) {
            msg += "/" + RID;
        }
        msg += " http://juick.com/" + MID;
        if (RID > 0) {
            msg += "#" + RID;
        }
        return msg;
    }
}
