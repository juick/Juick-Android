/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android.api;

import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ugnich
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
