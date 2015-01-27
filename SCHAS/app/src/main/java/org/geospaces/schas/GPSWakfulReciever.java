package org.geospaces.schas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;


public class GPSWakfulReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /*// This is the Intent to deliver to our service.
        Intent service = new Intent(context, GPSWakfulReciever.class);

        // Start the service, keeping the device awake while it is launching.
        Log.i("SimpleWakefulReceiver", "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);*/

        Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
    }
}