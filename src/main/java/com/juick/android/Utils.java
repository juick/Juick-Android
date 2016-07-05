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
package com.juick.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import com.juick.R;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author Ugnich Anton
 */
public class Utils {

    public static boolean hasAuth(Context context) {
        AccountManager am = AccountManager.get(context);
        Account accs[] = am.getAccountsByType(context.getString(R.string.com_juick));
        return accs.length > 0;
    }

    public static String getBasicAuthString(Context context) {
        AccountManager am = AccountManager.get(context);
        Account accs[] = am.getAccountsByType(context.getString(R.string.com_juick));
        if (accs.length > 0) {
            Bundle b = null;
            try {
                b = am.getAuthToken(accs[0], "", false, null, null).getResult();
            } catch (Exception e) {
                Log.e("getBasicAuthString", Log.getStackTraceString(e));
            }
            if (b != null) {
                String authStr = b.getString(AccountManager.KEY_ACCOUNT_NAME) + ":" + b.getString(AccountManager.KEY_AUTHTOKEN);
                return "Basic " + Base64.encodeToString(authStr.getBytes(), Base64.NO_WRAP);
            }
        }
        return "";
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
                str.append(line).append("\n");
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

    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWiFi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWiFi.isConnected();
    }

    private static WebSocketFactory WSFactoryInstance;

    public static WebSocketFactory getWSFactory() {
        if (WSFactoryInstance == null) {
            WSFactoryInstance = new WebSocketFactory();
        }
        return WSFactoryInstance;
    }
}
