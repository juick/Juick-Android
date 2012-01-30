/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.juick.R;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ugnich
 */
public class Utils {

    public static int doHttpGetRequest(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setUseCaches(false);
            conn.connect();
            int status = conn.getResponseCode();
            conn.disconnect();
            return status;
        } catch (Exception e) {
            Log.e("doHttpGetRequest", e.toString());
        }
        return 0;
    }

    public static boolean hasAuth(Context context) {
        AccountManager am = AccountManager.get(context);
        Account accs[] = am.getAccountsByType(context.getString(R.string.com_juick));
        return accs.length == 1;
    }

    public static String getAuthHash(Context context) {
        String jsonStr = getJSON(context, "http://api.juick.com/auth");
        if (jsonStr != null && !jsonStr.equals("")) {
            try {
                JSONObject json = new JSONObject(jsonStr);
                if (json.has("hash")) {
                    return json.getString("hash");
                }
            } catch (JSONException e) {
            }
        }
        return null;
    }

    public static String getBasicAuthString(Context context) {
        AccountManager am = AccountManager.get(context);
        Account accs[] = am.getAccountsByType(context.getString(R.string.com_juick));
        if (accs.length == 1) {
            String authStr = accs[0].name + ":" + am.getPassword(accs[0]);
            return "Basic " + Base64.encodeToString(authStr.getBytes(), Base64.NO_WRAP);
        } else {
            return "";
        }
    }

    public static String getJSON(Context context, String url) {
        String ret = null;
        try {
            URL jsonURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) jsonURL.openConnection();

            String basicAuth = getBasicAuthString(context);
            if (basicAuth.length() > 0) {
                conn.setRequestProperty("Authorization", basicAuth);
            }

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                ret = streamToString(conn.getInputStream());
            }

            conn.disconnect();
        } catch (Exception e) {
            Log.e("getJSON", e.toString());
        }
        return ret;
    }

    public static String postJSON(Context context, String url, String data) {
        String ret = null;
        try {
            URL jsonURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) jsonURL.openConnection();

            String basicAuth = getBasicAuthString(context);
            if (basicAuth.length() > 0) {
                conn.setRequestProperty("Authorization", basicAuth);
            }

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.connect();

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.close();

            if (conn.getResponseCode() == 200) {
                ret = streamToString(conn.getInputStream());
            }

            conn.disconnect();
        } catch (Exception e) {
            Log.e("getJSON", e.toString());
        }
        return ret;
    }

    public static String streamToString(InputStream is) {
        try {
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder str = new StringBuilder();
            String line;
            do {
                line = buf.readLine();
                str.append(line + "\n");
            } while (line != null);
            return str.toString();
        } catch (Exception e) {
            Log.e("streamReader", e.toString());
        }
        return null;
    }

    public static Bitmap downloadImage(String url) {
        try {
            URL imgURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imgURL.openConnection();
            conn.setDoInput(true);
            conn.connect();
            return BitmapFactory.decodeStream(conn.getInputStream());
        } catch (Exception e) {
            Log.e("downloadImage", e.toString());
        }
        return null;
    }
}
