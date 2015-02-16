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
import java.util.List;

import org.geospaces.schas.utils.*;

public class GPSWakfulReciever extends BroadcastReceiver {

    public static Activity  act          = null;
    public static Location  lastLocation = null;
    public static long      lastRecorded = -1;
    public static long      sessionNum    = 0;

    public static synchronized String storeLocation(Location loc, String... args) {
        File file = db.getFile(db.FILE_NAME);

        String dbg = (args.length > 0) ? args[0]: "";
        Log.w ("GPSWakful", "+" + dbg + "=>" + lastLocation + " " + loc);

        if ( lastLocation != null ) {
            float dist = Spatial.calculateDistance(  loc, lastLocation );
            Log.w("DIST", "****** "+ dist);
            if (dist < .1) {
                return "WARN: IGNORE: " + dist + " distance too short!";
            }
        }
        if (file.length() > db.FILE_SIZE) {
            if (!db.rename(false)) {
                return "ERROR: IGNORE: File Full: ";   // File is full and we can't do much now
            }
        }

        long curMinutes = loc.getTime()/1000000 * 60;

        if (lastLocation == null || (lastRecorded - curMinutes) > 10) {
            sessionNum = curMinutes;
        }
        lastLocation = loc;
        lastRecorded = loc.getTime()/1000000 * 60;
        Log.w("STORING:", " ===> "+ loc.getLatitude() + ":" + loc.getLongitude());

        String msg = db.getLocation(loc, ""+sessionNum,  args);

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), file.exists()));
            out.write(msg + "\n");
            out.close();
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to log file", e);
            msg = "ERROR: Exception appending to log file: " + e;
        }
        return "SUCCESS: " + msg;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle b = intent.getExtras();
        Location loc = (Location) b.get(LocationPoller.EXTRA_LOCATION);
        String msg;

        if (loc == null) {
            loc = (Location) b.get(LocationPoller.EXTRA_LASTKNOWN);
            if (loc == null) {
                msg = intent.getStringExtra(LocationPoller.EXTRA_ERROR);
                Log.e("GPS", msg);
            } else {
                msg = null; // "TIMEOUT, lastKnown=" + loc.toString();
            }
            return;
        }
        storeLocation(loc, "GPSWakeful:");
    }

    public void UploadDataMessage(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName act = taskInfo.get(0).topActivity;

        if ( act.getClassName().equals("UploadData") ) {
        }
        Log.d("HI", "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName()+"   Package Name :  "+act.getPackageName());
    }

}

