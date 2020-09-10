package com.juick.android;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

public class NotificationManager {
    public NotificationManager(Context context) {
        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                String token = instanceIdResult.getToken();
                Utils.updateFCMToken(token);
            });
        }
    }
    public void onResume() {

    }
}
