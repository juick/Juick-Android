package com.juick.android.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.juick.android.Utils;

/**
 * Created by vt on 03/12/15.
 */
public class RegistrationInstanceIDListenerService extends FirebaseInstanceIdService {
    private final static String TAG = "Juick-FCM";
    @Override
    public void onTokenRefresh() {
        final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        Utils.updateFCMToken();
    }
}
