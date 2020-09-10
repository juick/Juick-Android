package com.juick.android;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;

public class NotificationManager {
    public NotificationManager(Context context) {
        // get token
        new Thread() {
            @Override
            public void run() {
                try {
                    // read from agconnect-services.json
                    String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
                    String pushtoken = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
                    if (!TextUtils.isEmpty(pushtoken)) {
                        Log.i("HMS", "get token:" + pushtoken);

                    }
                } catch (Exception e) {
                    Log.i("HMS", "getToken failed, " + e);

                }
            }
        }.start();
    }
    public void onResume() {

    }
}
