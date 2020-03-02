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
package com.juick.android.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import com.juick.R;
import com.juick.android.SignInActivity;

/**
 *
 * @author Ugnich Anton
 */
public class AuthenticationService extends Service {

    private JuickAuthenticator accountAuthenticator;

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = accountAuthenticator.getIBinder();
        }
        return ret;
    }

    @Override
    public void onCreate() {
        accountAuthenticator = new JuickAuthenticator(getApplicationContext());
    }

    private static class JuickAuthenticator extends AbstractAccountAuthenticator {

        private final Context context;

        JuickAuthenticator(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            Intent i = new Intent(context, SignInActivity.class);
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            Bundle reply = new Bundle();
            reply.putParcelable(AccountManager.KEY_INTENT, i);
            return reply;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, context.getString(R.string.com_juick));
            result.putString(AccountManager.KEY_AUTHTOKEN, AccountManager.get(context).getUserData(account, "hash"));
            return result;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse arg0, Account arg1, String[] arg2) {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse arg0, Account arg1, String arg2, Bundle arg3) {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String arg0) {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse arg0, Account arg1, Bundle arg2) {
            return null;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse arg0, String arg1) {
            return null;
        }
    }
}
