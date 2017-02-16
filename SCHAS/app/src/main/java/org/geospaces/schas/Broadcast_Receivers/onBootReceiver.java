package org.geospaces.schas.Broadcast_Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.geospaces.schas.Services.LocationService;
import org.geospaces.schas.Services.StepCounter;

/**
 * Created by Erik on 3/15/2016.
 */

public class onBootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startLocationService = new Intent(context, LocationService.class);
        context.startService(startLocationService);

        Intent startPedometerService = new Intent(context, StepCounter.class);
        context.startService(startPedometerService);
    }
}
