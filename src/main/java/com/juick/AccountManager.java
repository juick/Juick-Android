package com.juick;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

/**
 * Created by gerc on 14.02.2016.
 */
public class AccountManager {

    public static boolean hasAuth() {
        android.accounts.AccountManager am = android.accounts.AccountManager.get(App.getInstance());
        Account accs[] = am.getAccountsByType(App.getInstance().getString(R.string.com_juick));
        return accs.length > 0;
    }

    public static String getNick() {
        android.accounts.AccountManager am = android.accounts.AccountManager.get(App.getInstance());
        Account accs[] = am.getAccountsByType(App.getInstance().getString(R.string.com_juick));
        return accs.length > 0 ? accs[0].name : null;
    }

    public static String getBasicAuthString() {
        Context context = App.getInstance();
        android.accounts.AccountManager am = android.accounts.AccountManager.get(context);
        Account accs[] = am.getAccountsByType(context.getString(R.string.com_juick));
        if (accs.length > 0) {
            Bundle b = null;
            try {
                b = am.getAuthToken(accs[0], "", null, false, null, null).getResult();
            } catch (Exception e) {
                Log.e("getBasicAuthString", Log.getStackTraceString(e));
            }
            if (b != null) {
                String authStr = b.getString(android.accounts.AccountManager.KEY_ACCOUNT_NAME) + ":" + b.getString(android.accounts.AccountManager.KEY_AUTHTOKEN);
                return "Basic " + Base64.encodeToString(authStr.getBytes(), Base64.NO_WRAP);
            }
        }
        return "";
    }
}
