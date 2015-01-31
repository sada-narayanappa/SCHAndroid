package org.geospaces.schas;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import com.commonsware.cwac.locpoll.LocationPoller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;


public class GPSWakfulReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + "/SCHAS");
        directory2.mkdirs();
        File log = new File(directory2, "LocationLogACTIVE.txt");

       /* File log=
                new File(Environment.getExternalStorageDirectory(),
                        "LocationLog.txt");*/

        try {
            BufferedWriter out=
                    new BufferedWriter(new FileWriter(log.getAbsolutePath(),
                            log.exists()));

            out.write(new Date().toString());
            out.write(" : ");

            Bundle b=intent.getExtras();
            Location loc=(Location)b.get(LocationPoller.EXTRA_LOCATION);
            String msg;

            if (loc==null) {
                loc=(Location)b.get(LocationPoller.EXTRA_LASTKNOWN);

                if (loc==null) {
                    msg=intent.getStringExtra(LocationPoller.EXTRA_ERROR);
                }
                else {
                    msg="TIMEOUT, lastKnown="+loc.toString();
                }
            }
            else {
                msg=loc.toString();
            }

            if (msg==null) {
                msg="Invalid broadcast received!";
            }

            out.write(msg);
            out.write("\n");
            out.close();
        }
        catch (IOException e) {
            Log.e(getClass().getName(), "Exception appending to log file", e);
        }
    }
}







//--------------OLD CODE BELOW---------------

/*import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;


//public class GPSWakfulReciever extends BroadcastReceiver {


//    PendingIntent launchIntent;


  //  @Override
   // public void onReceive(Context context, Intent intent) {
        /* This is the Intent to deliver to our service.
        Intent service = new Intent(context, GPSWakfulReciever.class);
        // Start the service, keeping the device awake while it is launching.
        Log.i("SimpleWakefulReceiver", "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);*/
       /* launchIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,launchIntent);
        //launchIntent.cancel();
        //launchIntent = null;
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Toast.makeText(context, "Lat:"+location.getLatitude()+"; Lon:" + location.getLongitude(), Toast.LENGTH_SHORT).show();
        Log.i("SimpleWakefulReceiver","Lat:"+location.getLatitude()+"; Lon:" + location.getLongitude() );*/
    //    Toast.makeText(context,"Alarm has been tripped",Toast.LENGTH_SHORT).show();


   // }
//}