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


    public static void storeLocation(Location loc) {
        File file = db.getFile(db.FILE_NAME);

        lastLocation = loc;
        float dist = Spatial.calculateDistance(  loc, lastLocation );

        if (file.length() > db.FILE_SIZE) {
            if (!db.rename(false)) {
                return;                 // File is full and we can't do much now
            }
        }
        String msg = db.getLocation(loc);
        String wmsg = msg + "\n";
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), file.exists()));
            out.write(wmsg);
            out.close();
        } catch (IOException e) {
            Log.e("ERR", "Exception appending to log file", e);
        }
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
        storeLocation(loc);
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

