package com.wix.reactnativenotifications.fcm;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceIdService;

import com.wix.reactnativenotifications.Defs;

/**
 * Instance-ID + token refreshing handling service. Contacts the FCM to fetch the updated token.
 *
 * @author amitd
 */
public class FcmInstanceIdListenerService extends FirebaseInstanceIdService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        // Google recommends running this from an intent service.
        try {
            Intent intent = new Intent(this, FcmInstanceIdRefreshHandlerService.class);
            startService(intent);
        } catch (Exception e) {
            Log.e(Defs.LOGTAG, "start fcm services failed" + e.getMessage());
        }
    }
}