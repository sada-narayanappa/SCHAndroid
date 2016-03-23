package org.geospaces.schas.Broadcast_Receivers;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.geospaces.schas.R;
import org.geospaces.schas.Services.LocationService;

/**
 * Created by Erik on 3/15/2016.
 */

public class onBootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startLocationService = new Intent(context, LocationService.class);
        context.startService(startLocationService);
    }
}
