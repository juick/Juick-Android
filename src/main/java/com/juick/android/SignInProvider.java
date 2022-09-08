/*
 * Copyright (C) 2008-2022, Juick
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

package com.juick.android;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.RelativeLayout;

public interface SignInProvider {
    interface SignInRequestCallback {
        void request(String username, String password);
    }
    interface SignInSuccessCallback {
        void response(String username, String hash);
    }
    View prepareSignIn(Activity context, RelativeLayout button);
    default void performSignIn() {};
    default void onSignInResult(int requestCode, int resultCode, Intent data,
                        SignInRequestCallback requestCallback, SignInSuccessCallback successCallback) {};
}
