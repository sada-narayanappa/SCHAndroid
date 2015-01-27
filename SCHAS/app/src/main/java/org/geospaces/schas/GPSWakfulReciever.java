package org.geospaces.schas;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;


public class GPSWakfulReciever extends BroadcastReceiver {
    @Override

    PendingIntent launchIntent;

    public void onReceive(Context context, Intent intent) {
        /*// This is the Intent to deliver to our service.
        Intent service = new Intent(context, GPSWakfulReciever.class);

        // Start the service, keeping the device awake while it is launching.
        Log.i("SimpleWakefulReceiver", "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);*/
        launchIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, launchIntent);
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,launchIntent);
        launchIntent.cancel();
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


        Toast.makeText(context, "Lat:"+location.getLatitude()+"; Lon:" + location.getLongitude(), Toast.LENGTH_SHORT).show();
    }
}