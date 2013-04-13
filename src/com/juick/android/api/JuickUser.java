/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
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

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class JuickUser {

    public int UID = 0;
    public String UName = null;
    public Object Avatar = null;
    public String FullName = null;

    public static JuickUser parseJSON(JSONObject json) throws JSONException {
        JuickUser juser = new JuickUser();
        if (json.has("uid")) {
            juser.UID = json.getInt("uid");
        }
        if (json.has("uname")) {
            juser.UName = json.getString("uname");
        }
        if (json.has("fullname")) {
            juser.FullName = json.getString("fullname");
        }
        return juser;
    }
}
