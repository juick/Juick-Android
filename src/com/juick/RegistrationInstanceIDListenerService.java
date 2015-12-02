package com.juick;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by vt on 03/12/15.
 */
public class RegistrationInstanceIDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        startService(new Intent(this, RegistrationIntentService.class));
    }
}
