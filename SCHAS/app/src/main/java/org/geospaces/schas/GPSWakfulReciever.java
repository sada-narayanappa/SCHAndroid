package org.geospaces.schas;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.commonsware.cwac.locpoll.LocationPoller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.geospaces.schas.utils.*;

public class GPSWakfulReciever extends BroadcastReceiver {

    public static Activity  act          = null;
    public static Location  lastLocation = null;
    public static long      lastRecorded = -1;
    public static long      sessionNum    = 0;
    public static double    minDistance   = 100;   // 100 meters

    static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");

    public static synchronized String storeLocation(Location loc, String source) {
        if ( lastLocation == null && SCHASSettings.location.getLatitude() != 0) {
            lastLocation = loc;
            lastRecorded = loc.getTime();
            sessionNum  = loc.getTime();
            return "GOT FIRST LOCATION";
        }
        File file = db.getFile(db.FILE_NAME);
        long curTime = loc.getTime();
        long timeFromLastReading = (curTime - lastRecorded);
        if (timeFromLastReading  <= 1000) {  // lets have at least a second
            String msg = "WARN: " + timeFromLastReading + " !" + sdf.format(loc.getTime()) + ": ACC:" + loc.getAccuracy();
            Log.w("IGNORING:", "**** "+ msg + " time:" + timeFromLastReading + " ACC:" + loc.getAccuracy());
            return msg;
        }
        double dist  = 1000 * Spatial.calculateDistance(  loc, lastLocation ); // meters
        double speed = (dist/timeFromLastReading) * 1000 ;

        if ( lastLocation != null ) {
            if (dist < minDistance) {
                String msg = "WARN: " + dist + " !" + sdf.format(loc.getTime()) + ": ACC:" + loc.getAccuracy();
                Log.w("IGNORING:", "**** "+ msg + " Dist:" +dist + " ACC:" + loc.getAccuracy());
                return msg;
            }
        }
        if (file.length() > db.FILE_SIZE) {
            if (!db.rename(false)) {
                Log.w("IGNORING:", "Log file is FULL ======");
                return "ERROR: IGNORE: File Full: ";   // File is full and we can't do much now
                // Actually we can append FILE to FILE_READY if FILE_READY size is small
                // Also - since we are interested in most recent data - we could remove old file
            }
        }

        if (lastLocation == null || (timeFromLastReading) > (10 * 60 * 1000)) {
            sessionNum = curTime;
            Log.w("GPSW", "Chosen a new Session Number: " + sessionNum);
        }
        if ( SCHASSettings.location.getLatitude() == 0) {
            SCHASSettings.location = loc;
            SCHASSettings.lastRecorded = lastRecorded;

            SCHASSettings.saveSettings();
        }
        Location l = (lastLocation == null) ? loc:lastLocation;
        String msg = loc.getLatitude() + "," + loc.getLongitude() + "-" +l.getLatitude() +
                "," + l.getLongitude();
        Log.w("STORING:", " ===> "+ msg + " Dist:" +dist);
        lastLocation = loc;
        lastRecorded = loc.getTime();

        String[] s = new String[2];
        msg = db.getLocation(loc, ""+ (sessionNum/1000),  source, speed);

        try {
            db.Write(msg + "\n");
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to log file", e);
            msg = "ERROR: Exception appending to log file: " + e;
            return "ERROR: " + msg;
        }
        return "SUCCESS: " + msg;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle b = intent.getExtras();
        Location loc = (Location) b.get(LocationPoller.EXTRA_LOCATION);

        if (loc == null) {
            //loc = (Location) b.get(LocationPoller.EXTRA_LASTKNOWN);
//            if (loc == null) {
//                String msg = intent.getStringExtra(LocationPoller.EXTRA_ERROR);
//                Log.e("GPS", msg);
//            }
            db.Upload(context, null);
            return;
        }
        storeLocation(loc, "GPSWakeful:");
        db.Upload(context, null);
    }
}

