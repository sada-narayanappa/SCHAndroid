package org.geospaces.schas.Services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.geospaces.schas.AsyncTasks.ReturnMobileIdExistsOnServer;

/**
 * Created by Student on 4/4/2017.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    public Context callingContext;

    private static final String TAG = "MyFirebaseIIDService";
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
    }
    // [END refresh_token]

    public String getToken()
    {
        final String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Token: " + token);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(callingContext);

        if (token != null) {
            if (!token.equals(sharedPref.getString("Firebase_token", ""))) {
                Handler mUIThreadHandler = new Handler(Looper.getMainLooper());
                mUIThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("Firebase_token", token);
                        editor.apply();

                        ReturnMobileIdExistsOnServer uploadTokenToServer = new ReturnMobileIdExistsOnServer();
                        uploadTokenToServer.context = callingContext;
                        uploadTokenToServer.firebaseToken = token;
                        uploadTokenToServer.execute();
                    }
                });
            }
        }

        return token;
    }
}