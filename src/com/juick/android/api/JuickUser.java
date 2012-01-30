/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ugnich
 */
public class JuickUser {

    public int UID = 0;
    public String UName = null;
    public Object Avatar = null;
    public String FullName = null;

    public static JuickUser parseJSON(JSONObject json) throws JSONException {
        JuickUser juser = new JuickUser();
        juser.UID = json.getInt("uid");
        juser.UName = json.getString("uname");
        if (json.has("fullname")) {
            juser.FullName = json.getString("fullname");
        }
        return juser;
    }
}
