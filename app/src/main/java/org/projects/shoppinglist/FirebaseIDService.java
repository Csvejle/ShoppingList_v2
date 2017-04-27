package org.projects.shoppinglist;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FirebaseIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";

    @Override
    public void onTokenRefresh() {
        //Får InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        //Usskriver token
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        //Kald til nedenstående metode
        sendRegistrationToServer(refreshedToken);
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token den nye token.
     */
    private void sendRegistrationToServer(String token) {
        //Brug af den nye token, hvis der er behov for det, hvilket vi ikke har.
    }
}