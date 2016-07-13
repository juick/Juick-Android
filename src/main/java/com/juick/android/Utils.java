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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import com.juick.App;
import com.juick.R;
import com.neovisionaries.ws.client.WebSocketFactory;

/**
 *
 * @author Ugnich Anton
 */
public class Utils {

    public static boolean hasAuth() {
        AccountManager am = AccountManager.get(App.getInstance());
        Account accs[] = am.getAccountsByType(App.getInstance().getString(R.string.com_juick));
        return accs.length > 0;
    }

    public static String getNick() {
        AccountManager am = AccountManager.get(App.getInstance());
        Account accs[] = am.getAccountsByType(App.getInstance().getString(R.string.com_juick));
        return accs.length > 0 ? accs[0].name : null;
    }

    public static String getBasicAuthString() {
        Context context = App.getInstance();
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
