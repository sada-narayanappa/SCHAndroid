package org.geospaces.schas.Services;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class SCHASFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        Log.i("firebase", "it worked");
    }

    @Override
    public void onDeletedMessages()
    {
        Log.i("firebase", "wot");
    }
}
