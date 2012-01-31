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

import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class JuickPlace {

    public int pid = 0;
    public double lat = 0;
    public double lon = 0;
    public String name = null;
    public String description = null;
    public int users = 0;
    public int messages = 0;
    public int distance = 0;
    public Vector<String> tags = new Vector<String>();

    public static JuickPlace parseJSON(JSONObject json) throws JSONException {
        JuickPlace jplace = new JuickPlace();

        jplace.pid = json.getInt("pid");
        jplace.lat = json.getDouble("lat");
        jplace.lon = json.getDouble("lon");
        jplace.name = json.getString("name").replace("&quot;", "\"");
        if (json.has("description")) {
            jplace.description = json.getString("description").replace("&quot;", "\"");
        }
        if (json.has("users")) {
            jplace.users = json.getInt("users");
        }
        if (json.has("messages")) {
            jplace.messages = json.getInt("messages");
        }
        if (json.has("distance")) {
            jplace.distance = json.getInt("distance");
        }
        if (json.has("tags")) {
            JSONArray tags = json.getJSONArray("tags");
            for (int n = 0; n < tags.length(); n++) {
                jplace.tags.add(tags.getString(n).replace("&quot;", "\""));
            }
        }

        return jplace;
    }
}
